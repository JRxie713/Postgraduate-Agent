package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试轮次表（每一轮 = 一道题库题 + 学生回答 + AI 点评）
 */
@Data
public class InterviewTurn {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 所属会话ID */
    private Integer sessionId;

    /** 第几轮(从1开始) */
    private Integer roundIndex;

    /** 对应题库ID(首轮/正常题有值,动态追问为空) */
    private Integer questionId;

    /** 当轮展示给学生的问题正文 */
    private String question;

    /** 是否由 AI 动态生成的追问(1=是,0=否) */
    private Integer isFollowup;

    /** 学生回答(文本) */
    private String studentAnswer;

    /** AI 对这一轮的点评(单题评语) */
    private String aiFeedback;

    /** AI 评 - 表达能力(0-100) */
    private Double aiExpression;

    /** AI 评 - 专业知识(0-100) */
    private Double aiProfessional;

    /** AI 评 - 逻辑思维(0-100) */
    private Double aiLogic;

    /** AI 评 - 应变能力(0-100) */
    private Double aiAdaptability;

    /** 本次评分是否为规则化兜底(1=是,无AI Key时) */
    private Integer ruleBased;

    /** 学生回答时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
