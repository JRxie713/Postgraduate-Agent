package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 面试超时提醒（前端轮询展示用）
 */
@Data
public class InterviewReminderVO {

    /** 会话ID */
    private Integer sessionId;

    /** 目标专业 */
    private String major;

    /** 训练类型 */
    private String trainingType;

    /** 已完成轮次 */
    private Integer currentRound;

    /** 计划轮次 */
    private Integer totalRounds;

    /** 已闲置分钟数 */
    private Long idleMinutes;

    /** 提示文案 */
    private String message;
}
