package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.entity.ExamQuestion;
import com.demo.scaffoldbackend.entity.PracticeRecord;
import com.demo.scaffoldbackend.entity.QuestionContent;
import com.demo.scaffoldbackend.entity.WrongQuestion;
import com.demo.scaffoldbackend.mapper.ExamQuestionMapper;
import com.demo.scaffoldbackend.mapper.PracticeRecordMapper;
import com.demo.scaffoldbackend.mapper.QuestionContentMapper;
import com.demo.scaffoldbackend.mapper.WrongQuestionMapper;
import com.demo.scaffoldbackend.service.PracticeQuestionSupport;
import com.demo.scaffoldbackend.service.WrongBookService;
import com.demo.scaffoldbackend.vo.WrongBookQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题本服务实现
 */
@Service
@RequiredArgsConstructor
public class WrongBookServiceImpl implements WrongBookService {

    private static final Set<String> MASTERY_LEVELS = Set.of("MASTERED", "UNMASTERED", "REVIEWING");

    private final WrongQuestionMapper wrongQuestionMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionContentMapper questionContentMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeQuestionSupport questionSupport;

    @Override
    public PageResult<WrongBookQuestionVO> pageWrongQuestions(Integer userId, int page, int size,
                                                              String examSubject, String knowledgePoint,
                                                              String masteryLevel) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(400, "页码必须大于0，每页数量必须在1到100之间");
        }
        LambdaQueryWrapper<WrongQuestion> wrapper = Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId)
                .eq(hasText(masteryLevel), WrongQuestion::getMasteryLevel, normalizeLevel(masteryLevel))
                .orderByDesc(WrongQuestion::getAddTime)
                .orderByDesc(WrongQuestion::getId);

        if (hasText(examSubject) || hasText(knowledgePoint)) {
            LambdaQueryWrapper<ExamQuestion> questionWrapper = Wrappers.<ExamQuestion>lambdaQuery()
                    .eq(hasText(examSubject), ExamQuestion::getExamSubject, examSubject.trim())
                    .eq(hasText(knowledgePoint), ExamQuestion::getKnowledgePoint, knowledgePoint.trim());
            List<Integer> questionIds = examQuestionMapper.selectList(questionWrapper).stream()
                    .map(ExamQuestion::getId).toList();
            if (questionIds.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            wrapper.in(WrongQuestion::getQuestionId, questionIds);
        }

        Page<WrongQuestion> result = wrongQuestionMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getTotal(), page, size, assemble(result.getRecords(), userId));
    }

    @Override
    public WrongBookQuestionVO getWrongQuestion(Integer userId, Integer id) {
        WrongQuestion wrongQuestion = requireOwned(userId, id);
        return assemble(List.of(wrongQuestion), userId).stream().findFirst().orElseThrow();
    }

    @Override
    @Transactional
    public WrongBookQuestionVO updateMastery(Integer userId, Integer id, String masteryLevel) {
        String level = normalizeLevel(masteryLevel);
        if (!MASTERY_LEVELS.contains(level)) {
            throw new BusinessException(400, "掌握状态仅支持 MASTERED / UNMASTERED / REVIEWING");
        }
        WrongQuestion wrongQuestion = requireOwned(userId, id);
        wrongQuestion.setMasteryLevel(level);
        wrongQuestion.setUpdateTime(LocalDateTime.now());
        wrongQuestionMapper.updateById(wrongQuestion);
        return getWrongQuestion(userId, id);
    }

    @Override
    public boolean deleteWrongQuestion(Integer userId, Integer id) {
        requireOwned(userId, id);
        return wrongQuestionMapper.deleteById(id) > 0;
    }

    private List<WrongBookQuestionVO> assemble(List<WrongQuestion> wrongQuestions, Integer userId) {
        if (wrongQuestions == null || wrongQuestions.isEmpty()) {
            return List.of();
        }
        List<Integer> questionIds = wrongQuestions.stream().map(WrongQuestion::getQuestionId).distinct().toList();
        Map<Integer, ExamQuestion> questions = examQuestionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity(), (a, b) -> a));
        List<Integer> contentIds = questions.values().stream().map(ExamQuestion::getContentId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, QuestionContent> contents = contentIds.isEmpty() ? Map.of()
                : questionContentMapper.selectBatchIds(contentIds).stream()
                .collect(Collectors.toMap(QuestionContent::getId, Function.identity(), (a, b) -> a));

        // 最近一次错误作答：用于错题详情展示“原答案”
        Map<Integer, PracticeRecord> lastWrongAnswers = new HashMap<>();
        List<PracticeRecord> records = practiceRecordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
                .eq(PracticeRecord::getUserId, userId)
                .in(PracticeRecord::getQuestionId, questionIds)
                .eq(PracticeRecord::getIsCorrect, Boolean.FALSE)
                .orderByDesc(PracticeRecord::getId));
        for (PracticeRecord record : records) {
            lastWrongAnswers.putIfAbsent(record.getQuestionId(), record);
        }

        List<WrongBookQuestionVO> result = new ArrayList<>();
        for (WrongQuestion wrongQuestion : wrongQuestions) {
            ExamQuestion question = questions.get(wrongQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            QuestionContent content = contents.get(question.getContentId());
            WrongBookQuestionVO vo = new WrongBookQuestionVO();
            vo.setId(wrongQuestion.getId());
            vo.setUserId(wrongQuestion.getUserId());
            vo.setQuestionId(wrongQuestion.getQuestionId());
            vo.setContentId(question.getContentId());
            vo.setExamSubject(question.getExamSubject());
            vo.setChapter(question.getChapter());
            vo.setKnowledgePoint(question.getKnowledgePoint());
            vo.setQType(question.getQType());
            vo.setDifficulty(question.getDifficulty());
            vo.setWrongReason(wrongQuestion.getWrongReason());
            vo.setMasteryLevel(wrongQuestion.getMasteryLevel());
            vo.setReviewCount(wrongQuestion.getReviewCount() == null ? 0 : wrongQuestion.getReviewCount());
            vo.setWrongCount(wrongQuestion.getWrongCount() == null ? 1 : wrongQuestion.getWrongCount());
            vo.setAddTime(wrongQuestion.getAddTime());
            vo.setUpdateTime(wrongQuestion.getUpdateTime());
            if (content != null) {
                vo.setQuestionText(content.getQuestionText());
                vo.setOptions(questionSupport.parseOptions(content.getOptionInfo()));
                vo.setRefAnswer(content.getRefAnswer());
                vo.setAnalysis(content.getAnalysis());
            }
            PracticeRecord lastWrong = lastWrongAnswers.get(wrongQuestion.getQuestionId());
            if (lastWrong != null) {
                vo.setLastUserAnswer(lastWrong.getUserAnswer());
                vo.setLastAnswerTime(lastWrong.getAnswerTime());
            }
            result.add(vo);
        }
        return result;
    }

    private WrongQuestion requireOwned(Integer userId, Integer id) {
        if (id == null) {
            throw new BusinessException(400, "错题ID不能为空");
        }
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(id);
        if (wrongQuestion == null) {
            throw new BusinessException(404, "错题不存在");
        }
        if (!Objects.equals(wrongQuestion.getUserId(), userId)) {
            throw new BusinessException(403, "无权操作他人的错题");
        }
        return wrongQuestion;
    }

    private String normalizeLevel(String masteryLevel) {
        return masteryLevel == null ? "" : masteryLevel.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
