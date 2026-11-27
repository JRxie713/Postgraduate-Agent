package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.AnswerInterviewDTO;
import com.demo.scaffoldbackend.dto.CreateInterviewSessionDTO;
import com.demo.scaffoldbackend.dto.FollowUpDTO;
import com.demo.scaffoldbackend.vo.EnglishReviewVO;
import com.demo.scaffoldbackend.vo.FinalInterviewReportVO;
import com.demo.scaffoldbackend.vo.InterviewReminderVO;
import com.demo.scaffoldbackend.vo.InterviewSessionVO;
import com.demo.scaffoldbackend.vo.InterviewTurnVO;

import java.util.List;

/**
 * 复试 AI 训练服务
 *
 * <p>题目来自 interview_question_bank 题库，大模型只负责评分 / 点评 / 追问 / 报告，
 * 未配置 AI Key 时全部走规则化兜底，保证演示可用。</p>
 */
public interface InterviewService {

    /** 开始一场模拟面试（生成会话 + 第一题） */
    InterviewSessionVO startSession(Integer userId, CreateInterviewSessionDTO dto);

    /** 查询会话详情 */
    InterviewSessionVO getSession(Integer sessionId);

    /** 查询某个用户的历史会话 */
    List<InterviewSessionVO> listSessions(Integer userId);

    /** 查询某用户超时未作答的面试会话（前端轮询展示提醒） */
    List<InterviewReminderVO> listIdleReminders(Integer userId);

    /** 放弃一场进行中的面试（不再提醒、不生成报告） */
    void abandonSession(Integer sessionId);

    /** 刷新会话活跃时间：用户点"继续作答"后调用，提醒重新计时 */
    void touchSession(Integer sessionId);

    /** 获取当前待作答的题目（没有则自动出下一题） */
    InterviewTurnVO getCurrentQuestion(Integer sessionId);

    /** 提交本轮回答，触发 AI 打分与点评 */
    InterviewTurnVO submitAnswer(Integer sessionId, AnswerInterviewDTO dto);

    /** 基于某轮回答生成压力追问 */
    InterviewTurnVO generateFollowUp(Integer sessionId, FollowUpDTO dto);

    /** 结束面试并生成五维报告 */
    FinalInterviewReportVO finishSession(Integer sessionId);

    /** 查询已有报告 */
    FinalInterviewReportVO getReport(Integer sessionId);

    /** 英语自我介绍批改 */
    EnglishReviewVO reviewEnglish(String introduction);
}
