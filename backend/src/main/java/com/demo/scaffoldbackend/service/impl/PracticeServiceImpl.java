package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.dto.PracticeAnswerDTO;
import com.demo.scaffoldbackend.entity.ExamQuestion;
import com.demo.scaffoldbackend.entity.PracticeRecord;
import com.demo.scaffoldbackend.entity.QuestionContent;
import com.demo.scaffoldbackend.entity.WrongQuestion;
import com.demo.scaffoldbackend.mapper.ExamQuestionMapper;
import com.demo.scaffoldbackend.mapper.PracticeRecordMapper;
import com.demo.scaffoldbackend.mapper.QuestionContentMapper;
import com.demo.scaffoldbackend.mapper.WrongQuestionMapper;
import com.demo.scaffoldbackend.service.PracticeQuestionSupport;
import com.demo.scaffoldbackend.service.PracticeService;
import com.demo.scaffoldbackend.vo.PracticeAnswerResultVO;
import com.demo.scaffoldbackend.vo.PracticeOptionsVO;
import com.demo.scaffoldbackend.vo.PracticeQuestionVO;
import com.demo.scaffoldbackend.vo.PracticeRecordDetailVO;
import com.demo.scaffoldbackend.vo.PracticeRecordVO;
import com.demo.scaffoldbackend.vo.PracticeSummaryVO;
import com.demo.scaffoldbackend.vo.QuestionOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 刷题训练服务实现
 *
 * <p>题目内容复用 question_content / exam_question，新增 practice_record 记录日常练习行为；
 * 答错时自动写入 wrong_question，重复答错累加错误次数。</p>
 */
@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private static final String MASTERY_MASTERED = "MASTERED";
    private static final String MASTERY_UNMASTERED = "UNMASTERED";

    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionContentMapper questionContentMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeQuestionSupport questionSupport;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<PracticeQuestionVO> pageQuestions(Integer userId, int page, int size, String examSubject,
                                                        String chapter, String knowledgePoint, String difficulty,
                                                        String keyword) {
        validatePage(page, size);
        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(hasText(examSubject), ExamQuestion::getExamSubject, trim(examSubject))
                .eq(hasText(chapter), ExamQuestion::getChapter, trim(chapter))
                .eq(hasText(knowledgePoint), ExamQuestion::getKnowledgePoint, trim(knowledgePoint))
                .eq(hasText(difficulty), ExamQuestion::getDifficulty, trim(difficulty));

        if (hasText(keyword)) {
            List<Integer> contentIds = questionContentMapper.selectList(Wrappers.<QuestionContent>lambdaQuery()
                            .select(QuestionContent::getId)
                            .like(QuestionContent::getQuestionText, keyword.trim()))
                    .stream().map(QuestionContent::getId).toList();
            if (contentIds.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            wrapper.in(ExamQuestion::getContentId, contentIds);
        }

        Page<ExamQuestion> result = examQuestionMapper.selectPage(Page.of(page, size),
                wrapper.orderByAsc(ExamQuestion::getId));
        return PageResult.of(result.getTotal(), page, size, assembleQuestions(result.getRecords(), userId));
    }

    @Override
    public PracticeQuestionVO getQuestion(Integer userId, Integer questionId) {
        ExamQuestion question = requireQuestion(questionId);
        QuestionContent content = requireContent(question.getContentId());
        return assembleQuestions(List.of(question), userId).stream().findFirst()
                .orElseGet(() -> buildQuestion(question, content, false));
    }

    @Override
    @Transactional
    public PracticeAnswerResultVO submitAnswer(Integer userId, PracticeAnswerDTO dto) {
        if (dto == null || dto.getQuestionId() == null) {
            throw new BusinessException(400, "题目ID不能为空");
        }
        if (!hasText(dto.getUserAnswer())) {
            throw new BusinessException(400, "请先选择答案");
        }
        ExamQuestion question = requireQuestion(dto.getQuestionId());
        QuestionContent content = requireContent(question.getContentId());
        List<QuestionOptionVO> options = questionSupport.parseOptions(content.getOptionInfo());
        boolean correct = questionSupport.judge(dto.getUserAnswer(), content.getRefAnswer(), options);

        LocalDateTime now = LocalDateTime.now();
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setContentId(question.getContentId());
        record.setExamSubject(question.getExamSubject());
        record.setChapter(question.getChapter());
        record.setKnowledgePoint(question.getKnowledgePoint());
        record.setQType(question.getQType());
        record.setDifficulty(question.getDifficulty());
        record.setUserAnswer(dto.getUserAnswer().trim());
        record.setCorrectAnswer(content.getRefAnswer());
        record.setIsCorrect(correct);
        record.setAnswerTime(now);
        record.setCreateTime(now);
        practiceRecordMapper.insert(record);

        WrongQuestion wrongQuestion = findWrongQuestion(userId, question.getId());
        boolean newlyAdded = false;
        if (wrongQuestion == null) {
            if (!correct) {
                wrongQuestion = new WrongQuestion();
                wrongQuestion.setUserId(userId);
                wrongQuestion.setQuestionId(question.getId());
                wrongQuestion.setWrongReason("日常练习答错");
                wrongQuestion.setMasteryLevel(MASTERY_UNMASTERED);
                wrongQuestion.setAddTime(now);
                wrongQuestion.setReviewCount(0);
                wrongQuestion.setWrongCount(1);
                wrongQuestion.setCreateTime(now);
                wrongQuestion.setUpdateTime(now);
                wrongQuestionMapper.insert(wrongQuestion);
                newlyAdded = true;
            }
        } else {
            int reviews = wrongQuestion.getReviewCount() == null ? 0 : wrongQuestion.getReviewCount();
            wrongQuestion.setReviewCount(reviews + 1);
            if (!correct) {
                int wrongCount = wrongQuestion.getWrongCount() == null ? 0 : wrongQuestion.getWrongCount();
                wrongQuestion.setWrongCount(wrongCount + 1);
                wrongQuestion.setWrongReason("日常练习答错");
                // 曾经标记已掌握但再次答错，重新回到未掌握
                wrongQuestion.setMasteryLevel(MASTERY_UNMASTERED);
            }
            wrongQuestion.setUpdateTime(now);
            wrongQuestionMapper.updateById(wrongQuestion);
        }

        PracticeAnswerResultVO vo = new PracticeAnswerResultVO();
        vo.setQuestionId(question.getId());
        vo.setUserAnswer(dto.getUserAnswer().trim());
        vo.setCorrectAnswer(content.getRefAnswer());
        vo.setCorrect(correct);
        vo.setAnalysis(content.getAnalysis());
        vo.setQuestionText(content.getQuestionText());
        vo.setNewlyAdded(newlyAdded);
        if (wrongQuestion != null) {
            vo.setWrongQuestionId(wrongQuestion.getId());
            vo.setWrongCount(wrongQuestion.getWrongCount() == null ? 1 : wrongQuestion.getWrongCount());
            vo.setMasteryLevel(wrongQuestion.getMasteryLevel());
        }
        return vo;
    }

    @Override
    public PageResult<PracticeRecordVO> pageRecords(Integer userId, int page, int size, String examSubject,
                                                    String knowledgePoint, Boolean onlyWrong, Boolean today) {
        validatePage(page, size);
        LambdaQueryWrapper<PracticeRecord> wrapper = Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getUserId, userId)
                .eq(hasText(examSubject), PracticeRecord::getExamSubject, trim(examSubject))
                .eq(hasText(knowledgePoint), PracticeRecord::getKnowledgePoint, trim(knowledgePoint))
                .eq(Boolean.TRUE.equals(onlyWrong), PracticeRecord::getIsCorrect, Boolean.FALSE)
                .ge(Boolean.TRUE.equals(today), PracticeRecord::getAnswerTime, LocalDate.now().atStartOfDay())
                .orderByDesc(PracticeRecord::getAnswerTime)
                .orderByDesc(PracticeRecord::getId);
        Page<PracticeRecord> result = practiceRecordMapper.selectPage(Page.of(page, size), wrapper);

        Map<Integer, QuestionContent> contents = loadContents(result.getRecords().stream()
                .map(PracticeRecord::getContentId).filter(java.util.Objects::nonNull).distinct().toList());
        List<PracticeRecordVO> records = result.getRecords().stream().map(item -> {
            PracticeRecordVO vo = new PracticeRecordVO();
            vo.setId(item.getId());
            vo.setQuestionId(item.getQuestionId());
            vo.setExamSubject(item.getExamSubject());
            vo.setChapter(item.getChapter());
            vo.setKnowledgePoint(item.getKnowledgePoint());
            vo.setQType(item.getQType());
            vo.setDifficulty(item.getDifficulty());
            vo.setUserAnswer(item.getUserAnswer());
            vo.setCorrectAnswer(item.getCorrectAnswer());
            vo.setCorrect(item.getIsCorrect());
            vo.setAnswerTime(item.getAnswerTime());
            QuestionContent content = contents.get(item.getContentId());
            if (content != null) {
                vo.setQuestionText(content.getQuestionText());
            }
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    @Override
    public PracticeRecordDetailVO recordDetail(Integer userId, Long id) {
        String cacheKey = "practice:record:detail:" + userId + ":" + id;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, PracticeRecordDetailVO.class);
            }
        } catch (Exception ignore) {
            // 缓存异常不影响主流程，回源数据库
        }

        PracticeRecord record = practiceRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "练习记录不存在");
        }
        if (!java.util.Objects.equals(record.getUserId(), userId)) {
            throw new BusinessException(403, "无权查看他人的练习记录");
        }
        QuestionContent content = record.getContentId() == null ? null
                : questionContentMapper.selectById(record.getContentId());

        PracticeRecordDetailVO vo = new PracticeRecordDetailVO();
        vo.setId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setExamSubject(record.getExamSubject());
        vo.setChapter(record.getChapter());
        vo.setKnowledgePoint(record.getKnowledgePoint());
        vo.setQType(record.getQType());
        vo.setDifficulty(record.getDifficulty());
        vo.setUserAnswer(record.getUserAnswer());
        vo.setCorrectAnswer(record.getCorrectAnswer());
        vo.setCorrect(record.getIsCorrect());
        vo.setAnswerTime(record.getAnswerTime());
        if (content != null) {
            vo.setQuestionText(content.getQuestionText());
            vo.setOptions(questionSupport.parseOptions(content.getOptionInfo()));
            vo.setAnalysis(content.getAnalysis());
        }
        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), Duration.ofMinutes(30));
        } catch (Exception ignore) {
            // Redis 不可用时仅降级为不走缓存
        }
        return vo;
    }

    @Override
    public PracticeSummaryVO summary(Integer userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long total = practiceRecordMapper.selectCount(Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getUserId, userId));
        long correct = practiceRecordMapper.selectCount(Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getIsCorrect, Boolean.TRUE));
        long today = practiceRecordMapper.selectCount(Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getUserId, userId)
                .ge(PracticeRecord::getAnswerTime, todayStart));
        long wrongTotal = wrongQuestionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId));
        long mastered = wrongQuestionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getMasteryLevel, MASTERY_MASTERED));

        PracticeSummaryVO vo = new PracticeSummaryVO();
        vo.setTotal((int) total);
        vo.setCorrectCount((int) correct);
        vo.setTodayCount((int) today);
        vo.setWrongTotal((int) wrongTotal);
        vo.setMasteredCount((int) mastered);
        vo.setAccuracy(total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0);
        return vo;
    }

    @Override
    public List<PracticeQuestionVO> similarQuestions(Integer userId, Integer questionId, int limit) {
        ExamQuestion origin = requireQuestion(questionId);
        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<ExamQuestion> similar = querySimilar(origin, true, safeLimit);
        if (similar.isEmpty()) {
            similar = querySimilar(origin, false, safeLimit);
        }
        if (similar.isEmpty() && hasText(origin.getExamSubject())) {
            similar = examQuestionMapper.selectList(Wrappers.<ExamQuestion>lambdaQuery()
                    .eq(ExamQuestion::getExamSubject, origin.getExamSubject())
                    .ne(ExamQuestion::getId, origin.getId())
                    .last("LIMIT " + safeLimit));
        }
        return assembleQuestions(similar, userId);
    }

    @Override
    public List<PracticeQuestionVO> dailyQuestions(Integer userId, int count, String examSubject, String difficulty) {
        int safeCount = Math.max(1, Math.min(count, 50));
        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .eq(hasText(examSubject), ExamQuestion::getExamSubject, trim(examSubject))
                .eq(hasText(difficulty), ExamQuestion::getDifficulty, trim(difficulty))
                .last("ORDER BY RAND() LIMIT " + safeCount);
        return assembleQuestions(examQuestionMapper.selectList(wrapper), userId);
    }

    @Override
    public PracticeOptionsVO options() {        List<ExamQuestion> questions = examQuestionMapper.selectList(Wrappers.<ExamQuestion>lambdaQuery()
                .select(ExamQuestion::getExamSubject, ExamQuestion::getChapter,
                        ExamQuestion::getKnowledgePoint, ExamQuestion::getDifficulty));

        Set<String> subjects = new LinkedHashSet<>();
        Set<String> difficulties = new LinkedHashSet<>();
        Map<String, Set<String>> chapterMap = new LinkedHashMap<>();
        Map<String, Set<String>> pointMap = new LinkedHashMap<>();
        for (ExamQuestion question : questions) {
            String subject = hasText(question.getExamSubject()) ? question.getExamSubject().trim() : "未分类";
            subjects.add(subject);
            if (hasText(question.getDifficulty())) {
                difficulties.add(question.getDifficulty().trim());
            }
            if (hasText(question.getChapter())) {
                chapterMap.computeIfAbsent(subject, key -> new LinkedHashSet<>()).add(question.getChapter().trim());
            }
            if (hasText(question.getKnowledgePoint())) {
                pointMap.computeIfAbsent(subject, key -> new LinkedHashSet<>()).add(question.getKnowledgePoint().trim());
            }
        }

        PracticeOptionsVO vo = new PracticeOptionsVO();
        vo.setSubjects(new ArrayList<>(subjects));
        vo.setDifficulties(new ArrayList<>(difficulties));
        vo.setChapters(toListMap(chapterMap));
        vo.setKnowledgePoints(toListMap(pointMap));
        return vo;
    }

    private List<ExamQuestion> querySimilar(ExamQuestion origin, boolean byKnowledgePoint, int limit) {
        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .ne(ExamQuestion::getId, origin.getId());
        if (byKnowledgePoint) {
            if (!hasText(origin.getKnowledgePoint())) {
                return List.of();
            }
            wrapper.eq(ExamQuestion::getKnowledgePoint, origin.getKnowledgePoint());
        } else {
            if (!hasText(origin.getChapter())) {
                return List.of();
            }
            wrapper.eq(ExamQuestion::getChapter, origin.getChapter());
        }
        wrapper.last("LIMIT " + limit);
        return examQuestionMapper.selectList(wrapper);
    }

    private List<PracticeQuestionVO> assembleQuestions(List<ExamQuestion> questions, Integer userId) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        List<Integer> questionIds = questions.stream().map(ExamQuestion::getId).toList();
        List<Integer> contentIds = questions.stream().map(ExamQuestion::getContentId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Integer, QuestionContent> contents = loadContents(contentIds);
        Set<Integer> wrongIds = userId == null ? Set.of()
                : wrongQuestionMapper.selectList(Wrappers.<WrongQuestion>lambdaQuery()
                        .eq(WrongQuestion::getUserId, userId)
                        .in(WrongQuestion::getQuestionId, questionIds))
                .stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());

        List<PracticeQuestionVO> result = new ArrayList<>();
        for (ExamQuestion question : questions) {
            QuestionContent content = contents.get(question.getContentId());
            if (content == null) {
                continue;
            }
            result.add(buildQuestion(question, content, wrongIds.contains(question.getId())));
        }
        return result;
    }

    private PracticeQuestionVO buildQuestion(ExamQuestion question, QuestionContent content, boolean wrong) {
        PracticeQuestionVO vo = new PracticeQuestionVO();
        vo.setId(question.getId());
        vo.setContentId(question.getContentId());
        vo.setExamSubject(question.getExamSubject());
        vo.setChapter(question.getChapter());
        vo.setKnowledgePoint(question.getKnowledgePoint());
        vo.setQType(question.getQType());
        vo.setDifficulty(question.getDifficulty());
        vo.setScore(question.getScore());
        vo.setQuestionText(content.getQuestionText());
        vo.setOptions(questionSupport.parseOptions(content.getOptionInfo()));
        vo.setWrong(wrong);
        return vo;
    }

    private Map<Integer, QuestionContent> loadContents(List<Integer> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Map.of();
        }
        return questionContentMapper.selectBatchIds(contentIds).stream()
                .collect(Collectors.toMap(QuestionContent::getId, Function.identity(), (a, b) -> a));
    }

    private WrongQuestion findWrongQuestion(Integer userId, Integer questionId) {
        List<WrongQuestion> list = wrongQuestionMapper.selectList(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId)
                .orderByAsc(WrongQuestion::getId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private ExamQuestion requireQuestion(Integer questionId) {
        if (questionId == null) {
            throw new BusinessException(400, "题目ID不能为空");
        }
        ExamQuestion question = examQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return question;
    }

    private QuestionContent requireContent(Integer contentId) {
        QuestionContent content = contentId == null ? null : questionContentMapper.selectById(contentId);
        if (content == null) {
            throw new BusinessException(404, "题目内容不存在，请检查题库初始化脚本");
        }
        return content;
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(400, "页码必须大于0，每页数量必须在1到100之间");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Map<String, List<String>> toListMap(Map<String, Set<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            List<String> list = new ArrayList<>(value);
            list.sort(Comparator.naturalOrder());
            result.put(key, list);
        });
        return result;
    }
}
