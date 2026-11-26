package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 个人学习概览响应体（Redis Hash 缓存）
 */
@Data
public class StudyOverviewVO {

    /** 用户 ID */
    private Integer userId;

    /** 连续打卡天数 */
    private Long streak;

    /** 上次打卡日期，yyyy-MM-dd；从未打卡为 null */
    private String lastDate;

    /** 今日累计学习分钟数 */
    private Long minutesToday;

    /** 历史累计学习分钟数（Kafka 消费端异步累加） */
    private Long totalMinutes;

    /** 连续打卡榜名次（从 1 开始；未上榜为 null） */
    private Long rank;

    /** 缓存读取时间戳（演示缓存命中） */
    private String cacheTime;
}
