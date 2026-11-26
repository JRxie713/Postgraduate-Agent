package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.AnswerInterviewDTO;
import com.demo.scaffoldbackend.dto.CreateInterviewSessionDTO;
import com.demo.scaffoldbackend.dto.EnglishReviewDTO;
import com.demo.scaffoldbackend.dto.FollowUpDTO;
import com.demo.scaffoldbackend.dto.InterviewRecordDTO;
import com.demo.scaffoldbackend.service.InterviewService;
import com.demo.scaffoldbackend.vo.EnglishReviewVO;
import com.demo.scaffoldbackend.vo.FinalInterviewReportVO;
import com.demo.scaffoldbackend.vo.InterviewRecordVO;
import com.demo.scaffoldbackend.vo.InterviewSessionVO;
import com.demo.scaffoldbackend.vo.InterviewTurnVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复试 AI 训练模块
 *
 * <p>端到端闭环：开始面试 → 多轮问答（AI 点评）→ 压力追问 → 结束生成五维报告；
 * 另提供英语自我介绍批改。题目来自题库，AI 只负责评分与点评。</p>
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    // ==================== 会话 / 问答 / 报告 ====================

    /** 开始一场模拟面试 */
    @PostMapping("/sessions")
    public Result<InterviewSessionVO> startSession(@Valid @RequestBody CreateInterviewSessionDTO dto) {
        return Result.success(interviewService.startSession(dto.getUserId(), dto));
    }

    /** 查询某用户的历史面试 */
    @GetMapping("/sessions")
    public Result<List<InterviewSessionVO>> listSessions(@RequestParam Integer userId) {
        return Result.success(interviewService.listSessions(userId));
    }

    /** 查询会话详情 */
    @GetMapping("/sessions/{id}")
    public Result<InterviewSessionVO> getSession(@PathVariable Integer id) {
        return Result.success(interviewService.getSession(id));
    }

    /** 获取当前待作答的题目 */
    @GetMapping("/sessions/{id}/question")
    public Result<InterviewTurnVO> getQuestion(@PathVariable Integer id) {
        return Result.success(interviewService.getCurrentQuestion(id));
    }

    /** 提交本轮回答（返回 AI 点评与评分） */
    @PostMapping("/sessions/{id}/answers")
    public Result<InterviewTurnVO> submitAnswer(@PathVariable Integer id,
                                                @Valid @RequestBody AnswerInterviewDTO dto) {
        dto.setSessionId(id);
        return Result.success(interviewService.submitAnswer(id, dto));
    }

    /** 基于某轮回答生成压力追问 */
    @PostMapping("/sessions/{id}/follow-up")
    public Result<InterviewTurnVO> followUp(@PathVariable Integer id,
                                            @Valid @RequestBody FollowUpDTO dto) {
        dto.setSessionId(id);
        return Result.success(interviewService.generateFollowUp(id, dto));
    }

    /** 结束面试并生成五维报告 */
    @PostMapping("/sessions/{id}/finish")
    public Result<FinalInterviewReportVO> finish(@PathVariable Integer id) {
        return Result.success(interviewService.finishSession(id));
    }

    /** 查询面试报告 */
    @GetMapping("/sessions/{id}/report")
    public Result<FinalInterviewReportVO> getReport(@PathVariable Integer id) {
        return Result.success(interviewService.getReport(id));
    }

    /** 英语自我介绍批改 */
    @PostMapping("/english-introduction/review")
    public Result<EnglishReviewVO> reviewEnglish(@Valid @RequestBody EnglishReviewDTO dto) {
        return Result.success(interviewService.reviewEnglish(dto.getIntroduction()));
    }

    // ==================== 历史训练记录（旧表 interview_record） ====================

    /** 根据用户ID查询复试训练记录列表 */
    @GetMapping("/user/{userId}")
    public Result<List<InterviewRecordVO>> listByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 分页查询复试训练记录列表 */
    @GetMapping
    public Result<PageResult<InterviewRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询复试训练记录 */
    @GetMapping("/{id}")
    public Result<InterviewRecordVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增复试训练记录 */
    @PostMapping
    public Result<InterviewRecordVO> create(@RequestBody InterviewRecordDTO recordDTO) {
        return null;
    }

    /** 更新复试训练记录 */
    @PutMapping("/{id}")
    public Result<InterviewRecordVO> update(@PathVariable Integer id, @RequestBody InterviewRecordDTO recordDTO) {
        return null;
    }

    /** 删除复试训练记录 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }
}
