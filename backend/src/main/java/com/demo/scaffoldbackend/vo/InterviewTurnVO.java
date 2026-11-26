package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单轮问答详情（含 AI 点评和评分）
 */
@Data
public class InterviewTurnVO {

    /** 轮次ID */
    private Integer id;

    /** 第几轮 */
    private Integer roundIndex;

    /** 题库ID(动态追问问卷为 null) */
    private Integer questionId;

    /** 问题正文 */
    private String question;

    /** 是否为 AI 动态追问 */
    private Integer isFollowup;

    /** 学生回答 */
    private String studentAnswer;

    /** AI 点评 */
    private String aiFeedback;

    /** 表达能力分 */
    private Double aiExpression;

    /** 专业知识分 */
    private Double aiProfessional;

    /** 逻辑思维分 */
    private Double aiLogic;

    /** 应变能力分 */
    private Double aiAdaptability;

    /** 规则化兜底标记 */
    private Integer ruleBased;

    /** 回答时间 */
    private LocalDateTime createTime;
}
