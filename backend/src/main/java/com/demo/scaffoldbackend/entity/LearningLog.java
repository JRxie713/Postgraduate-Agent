package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表11 学习日志表
 */
@Data
public class LearningLog {

    /** 主键 */
    private Integer id;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
