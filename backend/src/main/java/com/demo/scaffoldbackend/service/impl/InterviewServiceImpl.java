package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.AnswerInterviewDTO;
import com.demo.scaffoldbackend.dto.CreateInterviewSessionDTO;
import com.demo.scaffoldbackend.dto.FollowUpDTO;
import com.demo.scaffoldbackend.entity.InterviewQuestionBank;
import com.demo.scaffoldbackend.entity.InterviewReport;
import com.demo.scaffoldbackend.entity.InterviewSession;
import com.demo.scaffoldbackend.entity.InterviewTurn;
import com.demo.scaffoldbackend.mapper.InterviewQuestionBankMapper;
import com.demo.scaffoldbackend.mapper.InterviewReportMapper;
import com.demo.scaffoldbackend.mapper.InterviewSessionMapper;
import com.demo.scaffoldbackend.mapper.InterviewTurnMapper;
import com.demo.scaffoldbackend.service.AiChatClient;
import com.demo.scaffoldbackend.service.InterviewService;
import com.demo.scaffoldbackend.vo.EnglishReviewVO;
import com.demo.scaffoldbackend.vo.FinalInterviewReportVO;
import com.demo.scaffoldbackend.vo.InterviewSessionVO;
import com.demo.scaffoldbackend.vo.InterviewTurnVO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 复试 AI 训练服务实现
 *
 * <p>出题完全来自题库表，大模型只承担：单题点评打分、压力追问、英语批改、最终报告。
 * 无 Key / 调用失败时统一走规则化兜底（ruleBased=1），保证功能闭环可演示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final String STATUS_ONGOING = "ongoing";
    private static final String STATUS_FINISHED = "finished";

    /** JSON 工具（Jackson 3，线程安全） */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 五维权重：表达 / 专业 / 逻辑 / 英语 / 应变 */
    private static final double W_EXPRESSION = 0.20;
    private static final double W_PROFESSIONAL = 0.30;
    private static final double W_LOGIC = 0.20;
    private static final double W_ENGLISH = 0.15;
    private static final double W_ADAPTABILITY = 0.15;

    /** 逻辑连接词（规则化兜底打分用） */
    private static final List<String> LOGIC_WORDS = Arrays.asList(
            "首先", "其次", "然后", "最后", "第一", "第二", "第三",
            "因为", "所以", "因此", "另外", "不过", "总之", "综上");

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final InterviewReportMapper reportMapper;
    private final InterviewQuestionBankMapper questionMapper;
    private final AiChatClient aiChatClient;

    // ==================== 会话 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionVO startSession(Integer userId, CreateInterviewSessionDTO dto) {
        String type = normalizeType(dto.getTrainingType());
        String difficulty = normalizeDifficulty(dto.getDifficulty());
        boolean pressure = Boolean.TRUE.equals(dto.getPressureMode());

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setMajor(dto.getMajor().trim());
        session.setTrainingType(type);
        session.setDifficulty(difficulty);
        session.setPressureMode(pressure ? 1 : 0);
        session.setTotalRounds("english".equals(type) ? 2 : 3);
        session.setCurrentRound(0);
        session.setStatus(STATUS_ONGOING);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        createNextTurn(session, pressure);
        return toSessionVO(session);
    }

    @Override
    public InterviewSessionVO getSession(Integer sessionId) {
        return toSessionVO(requireSession(sessionId));
    }

    @Override
    public List<InterviewSessionVO> listSessions(Integer userId) {
        if (userId == null) {
            throw new BusinessException(400, "userId 不能为空");
        }
        return sessionMapper.selectList(Wrappers.<InterviewSession>lambdaQuery()
                        .eq(InterviewSession::getUserId, userId)
                        .orderByDesc(InterviewSession::getCreateTime))
                .stream().map(this::toSessionVO).collect(Collectors.toList());
    }

    // ==================== 问答 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTurnVO getCurrentQuestion(Integer sessionId) {
        InterviewSession session = requireSession(sessionId);
        InterviewTurn pending = findPendingTurn(sessionId);
        if (pending == null) {
            if (STATUS_FINISHED.equals(session.getStatus())) {
                throw new BusinessException(409, "该场面试已结束，请查看报告");
            }
            if (session.getCurrentRound() >= session.getTotalRounds()) {
                throw new BusinessException(409, "题目已全部答完，请结束面试生成报告");
            }
            pending = createNextTurn(session, session.getPressureMode() != null && session.getPressureMode() == 1);
        }
        return toTurnVO(pending);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTurnVO submitAnswer(Integer sessionId, AnswerInterviewDTO dto) {
        InterviewSession session = requireSession(sessionId);
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new BusinessException(409, "该场面试已结束，无法继续作答");
        }
        if (dto.getAnswer() == null || dto.getAnswer().trim().isEmpty()) {
            throw new BusinessException(400, "回答内容不能为空");
        }

        InterviewTurn turn = turnMapper.selectOne(Wrappers.<InterviewTurn>lambdaQuery()
                .eq(InterviewTurn::getSessionId, sessionId)
                .eq(InterviewTurn::getRoundIndex, dto.getRoundIndex())
                .last("limit 1"));
        if (turn == null) {
            throw new BusinessException(404, "第 " + dto.getRoundIndex() + " 轮不存在");
        }
        if (turn.getStudentAnswer() != null && !turn.getStudentAnswer().isBlank()) {
            throw new BusinessException(409, "第 " + dto.getRoundIndex() + " 轮已作答");
        }

        turn.setStudentAnswer(dto.getAnswer().trim());
        turn.setUpdateTime(LocalDateTime.now());
        gradeTurn(turn);
        turnMapper.updateById(turn);

        session.setCurrentRound(dto.getRoundIndex());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        return toTurnVO(turn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTurnVO generateFollowUp(Integer sessionId, FollowUpDTO dto) {
        InterviewSession session = requireSession(sessionId);
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new BusinessException(409, "该场面试已结束，无法追问");
        }

        InterviewTurn base = turnMapper.selectOne(Wrappers.<InterviewTurn>lambdaQuery()
                .eq(InterviewTurn::getSessionId, sessionId)
                .eq(InterviewTurn::getRoundIndex, dto.getRoundIndex())
                .last("limit 1"));
        if (base == null) {
            throw new BusinessException(404, "第 " + dto.getRoundIndex() + " 轮不存在");
        }
        if (base.getStudentAnswer() == null || base.getStudentAnswer().isBlank()) {
            throw new BusinessException(409, "请先回答第 " + dto.getRoundIndex() + " 轮再追问");
        }

        String question = buildFollowUpQuestion(session, base);

        int maxRound = currentMaxRound(sessionId);
        InterviewTurn turn = new InterviewTurn();
        turn.setSessionId(sessionId);
        turn.setRoundIndex(maxRound + 1);
        turn.setQuestionId(null);
        turn.setQuestion(question);
        turn.setIsFollowup(1);
        turn.setRuleBased(0);
        turn.setCreateTime(LocalDateTime.now());
        turn.setUpdateTime(LocalDateTime.now());
        turnMapper.insert(turn);

        session.setTotalRounds(session.getTotalRounds() + 1);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        return toTurnVO(turn);
    }

    // ==================== 报告 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinalInterviewReportVO finishSession(Integer sessionId) {
        InterviewSession session = requireSession(sessionId);

        List<InterviewTurn> turns = turnMapper.selectList(Wrappers.<InterviewTurn>lambdaQuery()
                .eq(InterviewTurn::getSessionId, sessionId)
                .orderByAsc(InterviewTurn::getRoundIndex));
        boolean anyAnswered = turns.stream()
                .anyMatch(t -> t.getStudentAnswer() != null && !t.getStudentAnswer().isBlank());
        if (!anyAnswered) {
            throw new BusinessException(409, "还没有任何作答记录，无法生成报告");
        }

        InterviewReport report = reportMapper.selectOne(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getSessionId, sessionId)
                .last("limit 1"));
        boolean isNew = report == null;
        if (isNew) {
            report = new InterviewReport();
            report.setSessionId(sessionId);
            report.setReportRead(0);
            report.setCreateTime(LocalDateTime.now());
        }

        InterviewReport result = buildReport(session, turns);
        report.setExpressionScore(result.getExpressionScore());
        report.setProfessionalScore(result.getProfessionalScore());
        report.setLogicScore(result.getLogicScore());
        report.setEnglishScore(result.getEnglishScore());
        report.setAdaptabilityScore(result.getAdaptabilityScore());
        report.setTotalScore(result.getTotalScore());
        report.setSummary(result.getSummary());
        report.setMainIssues(result.getMainIssues());
        report.setSuggestions(result.getSuggestions());
        report.setRuleBased(result.getRuleBased());
        report.setUpdateTime(LocalDateTime.now());

        if (isNew) {
            reportMapper.insert(report);
        } else {
            reportMapper.updateById(report);
        }

        session.setStatus(STATUS_FINISHED);
        session.setScore(result.getTotalScore());
        session.setFinishTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        return toReportVO(report);
    }

    @Override
    public FinalInterviewReportVO getReport(Integer sessionId) {
        InterviewReport report = reportMapper.selectOne(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getSessionId, sessionId)
                .last("limit 1"));
        if (report == null) {
            throw new BusinessException(404, "该场面试还没有生成报告，请先结束面试");
        }
        return toReportVO(report);
    }

    // ==================== 英语点评 ====================

    @Override
    public EnglishReviewVO reviewEnglish(String introduction) {
        String text = introduction == null ? "" : introduction.trim();
        if (text.isEmpty()) {
            throw new BusinessException(400, "自我介绍内容不能为空");
        }

        String system = "你是一位考研复试英语面试考官，请批改学生的英文自我介绍。"
                + "只输出 JSON，不要任何多余文字。";
        String user = "学生自我介绍如下：\n" + text + "\n\n"
                + "请严格输出 JSON：{\"vocabulary\":0-100的整数,\"grammar\":0-100的整数,"
                + "\"structure\":0-100的整数,\"overallComment\":\"总体评价（中文，80字以内）\","
                + "\"suggestions\":[\"具体改进建议1\",\"建议2\",\"建议3\"],"
                + "\"grammarMistakes\":[\"原句 -> 建议改写\"]}";

        JsonNode node = aiChatClient.parseJson(aiChatClient.chat(system, user));
        if (node != null && node.has("vocabulary")) {
            EnglishReviewVO vo = new EnglishReviewVO();
            vo.setVocabularyScore(clamp(node.path("vocabulary").asDouble(70)));
            vo.setGrammarScore(clamp(node.path("grammar").asDouble(70)));
            vo.setStructureScore(clamp(node.path("structure").asDouble(70)));
            vo.setTotalScore(round1(vo.getVocabularyScore() * 0.35
                    + vo.getGrammarScore() * 0.35 + vo.getStructureScore() * 0.30));
            vo.setOverallComment(textOrDefault(node.path("overallComment").asText(null),
                    "整体表达基本清晰，建议补充更多细节与个人特色。"));
            vo.setSuggestions(readStringList(node.path("suggestions")));
            vo.setGrammarMistakes(readStringList(node.path("grammarMistakes")));
            vo.setRuleBased(0);
            return vo;
        }
        return ruleBasedEnglishReview(text);
    }

    // ==================== 内部：出题 ====================

    /**
     * 创建下一轮的题目（从题库抽题）
     */
    private InterviewTurn createNextTurn(InterviewSession session, boolean pressure) {
        Set<Integer> usedIds = turnMapper.selectList(Wrappers.<InterviewTurn>lambdaQuery()
                        .eq(InterviewTurn::getSessionId, session.getId()))
                .stream().map(InterviewTurn::getQuestionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        InterviewQuestionBank question = pickQuestion(session.getMajor(), session.getTrainingType(),
                session.getDifficulty(), usedIds, pressure);
        if (question == null) {
            throw new BusinessException(404, "题库中没有匹配的题目，请先执行 docs/sql/modules/04-interview.sql 初始化题库");
        }

        InterviewTurn turn = new InterviewTurn();
        turn.setSessionId(session.getId());
        turn.setRoundIndex(currentMaxRound(session.getId()) + 1);
        turn.setQuestionId(question.getId());
        turn.setQuestion(question.getQuestion());
        turn.setIsFollowup(0);
        turn.setRuleBased(0);
        turn.setCreateTime(LocalDateTime.now());
        turn.setUpdateTime(LocalDateTime.now());
        turnMapper.insert(turn);
        return turn;
    }

    /**
     * 按（专业 → 类型 → 难度）逐级放宽条件抽一道未用过的题
     */
    private InterviewQuestionBank pickQuestion(String major, String category, String difficulty,
                                               Collection<Integer> excludeIds, boolean pressure) {
        List<String[]> attempts = new ArrayList<>();
        if ("english".equals(category)) {
            attempts.add(new String[]{null, "english", difficulty});
            attempts.add(new String[]{null, "english", null});
        } else if (pressure || "pressure".equals(category)) {
            attempts.add(new String[]{major, "pressure", difficulty});
            attempts.add(new String[]{major, "pressure", null});
            attempts.add(new String[]{null, "pressure", null});
        }
        attempts.add(new String[]{major, category, difficulty});
        attempts.add(new String[]{major, category, null});
        attempts.add(new String[]{null, category, null});
        attempts.add(new String[]{major, null, null});
        attempts.add(new String[]{null, null, null});

        for (String[] attempt : attempts) {
            InterviewQuestionBank found = queryOne(attempt[0], attempt[1], attempt[2], excludeIds);
            if (found != null) {
                return found;
            }
        }
        // 题目都用过了：允许重复抽题，避免会话卡死
        for (String[] attempt : attempts) {
            InterviewQuestionBank found = queryOne(attempt[0], attempt[1], attempt[2], null);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private InterviewQuestionBank queryOne(String major, String category, String difficulty,
                                           Collection<Integer> excludeIds) {
        var wrapper = Wrappers.<InterviewQuestionBank>lambdaQuery()
                .eq(InterviewQuestionBank::getEnabled, 1)
                .eq(InterviewQuestionBank::getIsFollowup, 0);
        if (major != null && !major.isBlank()) {
            wrapper.eq(InterviewQuestionBank::getMajor, major);
        }
        if (category != null) {
            wrapper.eq(InterviewQuestionBank::getCategory, category);
        }
        if (difficulty != null) {
            wrapper.eq(InterviewQuestionBank::getDifficulty, difficulty);
        }
        if (excludeIds != null && !excludeIds.isEmpty()) {
            wrapper.notIn(InterviewQuestionBank::getId, excludeIds);
        }
        wrapper.last("order by rand() limit 1");
        return questionMapper.selectOne(wrapper);
    }

    // ==================== 内部：评分 ====================

    /**
     * 单题评分：优先大模型，失败走规则化
     */
    private void gradeTurn(InterviewTurn turn) {
        JsonNode node = aiChatClient.parseJson(aiChatClient.chat(
                GRADE_SYSTEM_PROMPT, buildGradeUserPrompt(turn)));
        if (node != null && node.has("expression")) {
            turn.setAiExpression(clamp(node.path("expression").asDouble(70)));
            turn.setAiProfessional(clamp(node.path("professional").asDouble(70)));
            turn.setAiLogic(clamp(node.path("logic").asDouble(70)));
            turn.setAiAdaptability(clamp(node.path("adaptability").asDouble(70)));
            turn.setAiFeedback(textOrDefault(node.path("feedback").asText(null),
                    "回答已完成，建议结合具体例子展开。"));
            turn.setRuleBased(0);
            return;
        }
        applyRuleBasedGrade(turn);
    }

    private static final String GRADE_SYSTEM_PROMPT =
            "你是考研复试面试官。请对学生的回答打分并点评，只输出 JSON，不要多余文字。"
                    + "评分标准：expression 表达清晰度、professional 专业知识、logic 逻辑结构、adaptability 应变能力，"
                    + "四项均为 0-100 的整数；feedback 为中文点评（100 字以内，指出优点与不足）。";

    private String buildGradeUserPrompt(InterviewTurn turn) {
        return "面试题目：" + turn.getQuestion() + "\n"
                + "学生回答：" + turn.getStudentAnswer() + "\n\n"
                + "请严格输出 JSON：{\"expression\":整数,\"professional\":整数,\"logic\":整数,"
                + "\"adaptability\":整数,\"feedback\":\"中文点评\"}";
    }

    /**
     * 规则化兜底评分：长度 + 逻辑词 + 要点命中
     */
    private void applyRuleBasedGrade(InterviewTurn turn) {
        String answer = turn.getStudentAnswer() == null ? "" : turn.getStudentAnswer().trim();
        int length = answer.length();

        double base = 45;
        if (length >= 30) {
            base += 10;
        }
        if (length >= 80) {
            base += 10;
        }
        if (length >= 150) {
            base += 5;
        }

        long logicHits = LOGIC_WORDS.stream().filter(answer::contains).count();
        double logicBonus = Math.min(15, logicHits * 5);

        long keyHits = 0;
        InterviewQuestionBank bank = turn.getQuestionId() == null ? null
                : questionMapper.selectById(turn.getQuestionId());
        if (bank != null && bank.getKeyPoints() != null) {
            keyHits = Arrays.stream(bank.getKeyPoints().split("[,，]"))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty() && answer.contains(k))
                    .count();
        }
        double keyBonus = Math.min(20, keyHits * 7);

        double professional = clamp(base + keyBonus);
        double logic = clamp(base + logicBonus + keyBonus * 0.5);
        double expression = clamp(base + Math.min(10, length / 25.0));
        double adaptability = clamp(base + (keyHits > 0 && logicHits > 0 ? 8 : 0));

        turn.setAiExpression(round1(expression));
        turn.setAiProfessional(round1(professional));
        turn.setAiLogic(round1(logic));
        turn.setAiAdaptability(round1(adaptability));
        turn.setAiFeedback(buildRuleFeedback(length, keyHits, logicHits));
        turn.setRuleBased(1);
    }

    private String buildRuleFeedback(int length, long keyHits, long logicHits) {
        StringBuilder sb = new StringBuilder("【规则化点评】");
        sb.append(length < 30 ? "回答偏简短，建议展开陈述。" : "回答篇幅充分。");
        if (keyHits == 0) {
            sb.append("未覆盖题目核心要点，建议围绕考察点作答。");
        } else {
            sb.append("命中 ").append(keyHits).append(" 个考察要点。");
        }
        if (logicHits == 0) {
            sb.append("建议使用“首先/其次/最后”等结构词组织答案。");
        } else {
            sb.append("答题结构较清晰。");
        }
        return sb.toString();
    }

    // ==================== 内部：追问 ====================

    private String buildFollowUpQuestion(InterviewSession session, InterviewTurn base) {
        String system = "你是考研复试面试官，请针对学生刚才的回答提出一个追问，只输出 JSON。";
        String user = "原问题：" + base.getQuestion() + "\n"
                + "学生回答：" + base.getStudentAnswer() + "\n"
                + (session.getPressureMode() != null && session.getPressureMode() == 1
                ? "学生开启了压力面试，请提出更犀利、更有挑战性的追问。\n" : "")
                + "请严格输出 JSON：{\"question\":\"追问内容\"}";

        JsonNode node = aiChatClient.parseJson(aiChatClient.chat(system, user));
        if (node != null && node.hasNonNull("question")) {
            String q = node.path("question").asText().trim();
            if (!q.isEmpty()) {
                return q;
            }
        }
        return ruleBasedFollowUp(session, base);
    }

    /**
     * 规则化追问：套用追问模板，把 {keyword} 替换为考察要点
     */
    private String ruleBasedFollowUp(InterviewSession session, InterviewTurn base) {
        String keyword = "刚才的内容";
        InterviewQuestionBank bank = base.getQuestionId() == null ? null
                : questionMapper.selectById(base.getQuestionId());
        if (bank != null && bank.getKeyPoints() != null && !bank.getKeyPoints().isBlank()) {
            keyword = bank.getKeyPoints().split("[,，]")[0].trim();
        }

        InterviewQuestionBank template = queryOne(session.getMajor(), "pressure", null, null);
        if (template != null && template.getIsFollowup() != null && template.getIsFollowup() == 1) {
            return template.getQuestion().replace("{keyword}", keyword);
        }
        return "你刚才提到“" + keyword + "”，能结合具体经历再展开说明一下吗？";
    }

    // ==================== 内部：报告 ====================

    private InterviewReport buildReport(InterviewSession session, List<InterviewTurn> turns) {
        List<InterviewTurn> graded = turns.stream()
                .filter(t -> t.getAiExpression() != null).collect(Collectors.toList());

        double expression = avg(graded, InterviewTurn::getAiExpression);
        double professional = avg(graded, InterviewTurn::getAiProfessional);
        double logic = avg(graded, InterviewTurn::getAiLogic);
        double adaptability = avg(graded, InterviewTurn::getAiAdaptability);
        double english = expression;

        InterviewReport report = new InterviewReport();
        report.setExpressionScore(round1(expression));
        report.setProfessionalScore(round1(professional));
        report.setLogicScore(round1(logic));
        report.setEnglishScore(round1(english));
        report.setAdaptabilityScore(round1(adaptability));
        report.setTotalScore(round1(expression * W_EXPRESSION + professional * W_PROFESSIONAL
                + logic * W_LOGIC + english * W_ENGLISH + adaptability * W_ADAPTABILITY));

        JsonNode node = aiChatClient.parseJson(aiChatClient.chat(
                REPORT_SYSTEM_PROMPT, buildReportUserPrompt(session, graded)));
        if (node != null && node.has("summary")) {
            report.setExpressionScore(clamp(node.path("expression").asDouble(report.getExpressionScore())));
            report.setProfessionalScore(clamp(node.path("professional").asDouble(report.getProfessionalScore())));
            report.setLogicScore(clamp(node.path("logic").asDouble(report.getLogicScore())));
            report.setEnglishScore(clamp(node.path("english").asDouble(report.getEnglishScore())));
            report.setAdaptabilityScore(clamp(node.path("adaptability").asDouble(report.getAdaptabilityScore())));
            report.setTotalScore(round1(report.getExpressionScore() * W_EXPRESSION
                    + report.getProfessionalScore() * W_PROFESSIONAL
                    + report.getLogicScore() * W_LOGIC
                    + report.getEnglishScore() * W_ENGLISH
                    + report.getAdaptabilityScore() * W_ADAPTABILITY));
            report.setSummary(node.path("summary").asText(""));
            report.setMainIssues(joinJson(readStringList(node.path("mainIssues"))));
            report.setSuggestions(joinJson(readStringList(node.path("suggestions"))));
            report.setRuleBased(0);
            return report;
        }

        report.setSummary("本场面试共作答 " + graded.size() + " 轮，总分 "
                + report.getTotalScore() + " 分。整体表现"
                + (report.getTotalScore() >= 80 ? "较好" : report.getTotalScore() >= 60 ? "中等" : "仍需加强")
                + "，建议针对薄弱维度继续专项训练。");
        report.setMainIssues(joinJson(ruleBasedIssues(report)));
        report.setSuggestions(joinJson(ruleBasedSuggestions(report)));
        report.setRuleBased(1);
        return report;
    }

    private static final String REPORT_SYSTEM_PROMPT =
            "你是考研复试面试专家。请根据多轮问答记录生成面试报告，只输出 JSON，不要多余文字。"
                    + "五个维度均为 0-100 的整数：expression 表达、professional 专业、logic 逻辑、"
                    + "english 英语、adaptability 应变。";

    private String buildReportUserPrompt(InterviewSession session, List<InterviewTurn> turns) {
        StringBuilder sb = new StringBuilder();
        sb.append("专业：").append(session.getMajor())
                .append("；难度：").append(session.getDifficulty())
                .append("；压力面试：").append(session.getPressureMode() != null && session.getPressureMode() == 1 ? "是" : "否")
                .append("\n问答记录：\n");
        for (InterviewTurn t : turns) {
            sb.append("第").append(t.getRoundIndex()).append("轮\n")
                    .append("问：").append(t.getQuestion()).append("\n")
                    .append("答：").append(t.getStudentAnswer()).append("\n");
        }
        sb.append("\n请严格输出 JSON：{\"expression\":整数,\"professional\":整数,\"logic\":整数,"
                + "\"english\":整数,\"adaptability\":整数,\"summary\":\"综合总评（中文，150字以内）\","
                + "\"mainIssues\":[\"主要问题1\",\"主要问题2\"],"
                + "\"suggestions\":[\"改进建议1\",\"建议2\",\"建议3\"]}");
        return sb.toString();
    }

    private List<String> ruleBasedIssues(InterviewReport report) {
        List<String> issues = new ArrayList<>();
        if (report.getProfessionalScore() < 70) {
            issues.add("专业知识深度不足，回答缺少关键概念与细节");
        }
        if (report.getLogicScore() < 70) {
            issues.add("答题逻辑结构不清，缺少层次与递进");
        }
        if (report.getExpressionScore() < 70) {
            issues.add("表达不够流畅，语句组织需要加强");
        }
        if (report.getAdaptabilityScore() < 70) {
            issues.add("临场应变偏弱，面对追问容易卡顿");
        }
        if (issues.isEmpty()) {
            issues.add("整体表现稳定，未发现明显短板");
        }
        return issues;
    }

    private List<String> ruleBasedSuggestions(InterviewReport report) {
        List<String> list = new ArrayList<>();
        if (report.getProfessionalScore() < 80) {
            list.add("按专业核心课程梳理高频复试考点，每个考点准备 2-3 个实例");
        }
        if (report.getLogicScore() < 80) {
            list.add("练习“总—分—总”答题结构，先说结论再展开论证");
        }
        if (report.getExpressionScore() < 80) {
            list.add("每天录音复述一道面试题，控制语速并减少口头语");
        }
        if (report.getAdaptabilityScore() < 80) {
            list.add("开启压力面试模式多练追问，训练临场反应");
        }
        if (list.isEmpty()) {
            list.add("保持当前水平，重点打磨自我介绍与科研经历表述");
        }
        return list;
    }

    // ==================== 内部：英语规则化兜底 ====================

    private EnglishReviewVO ruleBasedEnglishReview(String text) {
        String[] sentences = text.split("[.!?]+");
        List<String> words = Arrays.stream(text.toLowerCase().split("[^a-zA-Z']+"))
                .filter(w -> !w.isBlank()).collect(Collectors.toList());

        int wordCount = words.size();
        int sentenceCount = Math.max(1, sentences.length);
        long unique = words.stream().distinct().count();
        double diversity = wordCount == 0 ? 0 : (double) unique / wordCount;
        double avgLen = (double) wordCount / sentenceCount;

        double vocabulary = clamp(50 + diversity * 50);
        double grammar = 78;
        double structure = 65;

        List<String> mistakes = new ArrayList<>();
        if (text.contains("  ")) {
            mistakes.add("存在连续空格，注意排版规范");
            grammar -= 3;
        }
        if (text.matches("(?s).*\\bi\\b.*")) {
            mistakes.add("第一人称应大写：i -> I");
            grammar -= 5;
        }
        if (!text.trim().isEmpty() && Character.isLowerCase(text.trim().charAt(0))) {
            mistakes.add("首句首字母应大写");
            grammar -= 3;
        }
        if (avgLen > 30) {
            mistakes.add("句子过长，建议拆分为多个短句");
            structure -= 8;
        }

        Set<String> structureWords = new LinkedHashSet<>(Arrays.asList(
                "first", "second", "finally", "however", "moreover", "therefore", "in addition", "in conclusion"));
        long structureHits = structureWords.stream().filter(text.toLowerCase()::contains).count();
        structure += Math.min(20, structureHits * 7);

        if (wordCount < 60) {
            structure -= 8;
        }

        EnglishReviewVO vo = new EnglishReviewVO();
        vo.setVocabularyScore(round1(vocabulary));
        vo.setGrammarScore(round1(clamp(grammar)));
        vo.setStructureScore(round1(clamp(structure)));
        vo.setTotalScore(round1(vo.getVocabularyScore() * 0.35
                + vo.getGrammarScore() * 0.35 + vo.getStructureScore() * 0.30));
        vo.setOverallComment("【规则化点评】共 " + wordCount + " 词、" + sentenceCount
                + " 句，平均句长 " + round1(avgLen) + " 词，词汇多样性 " + Math.round(diversity * 100) + "%。"
                + (wordCount < 60 ? "篇幅偏短，建议补充科研经历与报考动机。" : "篇幅合理。"));
        List<String> suggestions = new ArrayList<>();
        if (wordCount < 60) {
            suggestions.add("扩写到 120-150 词，覆盖教育背景、科研经历、报考动机三部分");
        }
        if (structureHits == 0) {
            suggestions.add("使用 First / Moreover / Finally 等衔接词增强结构");
        }
        if (diversity < 0.55) {
            suggestions.add("替换重复词汇，增加同义表达提升词汇丰富度");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("整体表达不错，注意语速与停顿，可配合模拟问答练习");
        }
        vo.setSuggestions(suggestions);
        vo.setGrammarMistakes(mistakes);
        vo.setRuleBased(1);
        return vo;
    }

    // ==================== 内部：工具 ====================

    private InterviewSession requireSession(Integer sessionId) {
        if (sessionId == null) {
            throw new BusinessException(400, "sessionId 不能为空");
        }
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "面试会话不存在");
        }
        return session;
    }

    private InterviewTurn findPendingTurn(Integer sessionId) {
        return turnMapper.selectOne(Wrappers.<InterviewTurn>lambdaQuery()
                .eq(InterviewTurn::getSessionId, sessionId)
                .and(w -> w.isNull(InterviewTurn::getStudentAnswer)
                        .or().eq(InterviewTurn::getStudentAnswer, ""))
                .orderByAsc(InterviewTurn::getRoundIndex)
                .last("limit 1"));
    }

    private int currentMaxRound(Integer sessionId) {
        List<InterviewTurn> list = turnMapper.selectList(Wrappers.<InterviewTurn>lambdaQuery()
                .eq(InterviewTurn::getSessionId, sessionId)
                .orderByDesc(InterviewTurn::getRoundIndex)
                .last("limit 1"));
        return list.isEmpty() ? 0 : list.get(0).getRoundIndex();
    }

    private String normalizeType(String type) {
        String t = type == null ? "comprehensive" : type.trim().toLowerCase();
        List<String> valid = Arrays.asList("comprehensive", "professional", "pressure", "english");
        if (!valid.contains(t)) {
            throw new BusinessException(400, "训练类型无效，可选值：" + valid);
        }
        return t;
    }

    private String normalizeDifficulty(String difficulty) {
        String d = difficulty == null ? "medium" : difficulty.trim().toLowerCase();
        List<String> valid = Arrays.asList("easy", "medium", "hard");
        return valid.contains(d) ? d : "medium";
    }

    private double avg(List<InterviewTurn> turns,
                       java.util.function.Function<InterviewTurn, Double> getter) {
        List<Double> values = turns.stream().map(getter)
                .filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (values.isEmpty()) {
            return 0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> {
                String s = n.asText("").trim();
                if (!s.isEmpty()) {
                    list.add(s);
                }
            });
        }
        return list;
    }

    private String joinJson(List<String> list) {
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            return String.join("\n", list);
        }
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private InterviewSessionVO toSessionVO(InterviewSession s) {
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(s.getId());
        vo.setUserId(s.getUserId());
        vo.setMajor(s.getMajor());
        vo.setTrainingType(s.getTrainingType());
        vo.setDifficulty(s.getDifficulty());
        vo.setPressureMode(s.getPressureMode());
        vo.setTotalRounds(s.getTotalRounds());
        vo.setCurrentRound(s.getCurrentRound());
        vo.setStatus(s.getStatus());
        vo.setScore(s.getScore());
        vo.setCreateTime(s.getCreateTime());
        vo.setFinishTime(s.getFinishTime());
        return vo;
    }

    private InterviewTurnVO toTurnVO(InterviewTurn t) {
        InterviewTurnVO vo = new InterviewTurnVO();
        vo.setId(t.getId());
        vo.setRoundIndex(t.getRoundIndex());
        vo.setQuestionId(t.getQuestionId());
        vo.setQuestion(t.getQuestion());
        vo.setIsFollowup(t.getIsFollowup());
        vo.setStudentAnswer(t.getStudentAnswer());
        vo.setAiFeedback(t.getAiFeedback());
        vo.setAiExpression(t.getAiExpression());
        vo.setAiProfessional(t.getAiProfessional());
        vo.setAiLogic(t.getAiLogic());
        vo.setAiAdaptability(t.getAiAdaptability());
        vo.setRuleBased(t.getRuleBased());
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }

    private FinalInterviewReportVO toReportVO(InterviewReport r) {
        FinalInterviewReportVO vo = new FinalInterviewReportVO();
        vo.setId(r.getId());
        vo.setSessionId(r.getSessionId());
        FinalInterviewReportVO.Score score = new FinalInterviewReportVO.Score();
        score.setExpression(r.getExpressionScore());
        score.setProfessional(r.getProfessionalScore());
        score.setLogic(r.getLogicScore());
        score.setEnglish(r.getEnglishScore());
        score.setAdaptability(r.getAdaptabilityScore());
        vo.setScore(score);
        vo.setTotalScore(r.getTotalScore());
        vo.setSummary(r.getSummary());
        vo.setMainIssues(splitJson(r.getMainIssues()));
        vo.setSuggestions(splitJson(r.getSuggestions()));
        vo.setRuleBased(r.getRuleBased());
        vo.setCreateTime(r.getCreateTime() == null ? null : r.getCreateTime().toString());
        return vo;
    }

    private List<String> splitJson(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = JSON.readTree(text);
            List<String> list = readStringList(node);
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
            // 非 JSON 时按换行拆分
        }
        return Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
