package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习日志响应体
 */
@Data
public class LearningLogVO {

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
