package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话概览（前端首页/历史列表展示用）
 */
@Data
public class InterviewSessionVO {

    /** 会话ID */
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 训练目标专业 */
    private String major;

    /** 训练类型 */
    private String trainingType;

    /** 难度 */
    private String difficulty;

    /** 是否开启压力面试 */
    private Integer pressureMode;

    /** 计划轮次 */
    private Integer totalRounds;

    /** 已完成轮次 */
    private Integer currentRound;

    /** 会话状态 */
    private String status;

    /** 最终总分 */
    private Double score;

    /** 开始时间 */
    private LocalDateTime createTime;

    /** 结束时间 */
    private LocalDateTime finishTime;
}
