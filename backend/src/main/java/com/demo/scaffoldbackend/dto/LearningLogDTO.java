package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习日志新增/更新请求体
 */
@Data
public class LearningLogDTO {

    /** 用户ID */
    private Integer userId;

    /** 记录时间 */
    private LocalDateTime logDate;

    /** 学习时长 */
    private Integer duration;

    /** 专注度 */
    private Double focus;

    /** 情绪标签 */
    private String emotionTag;

    /** 是否打卡 */
    private Boolean checkIn;
}
