package com.demo.scaffoldbackend.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习打卡事件（Kafka 消息体）
 *
 * <p>主流程在 Redis 中完成实时读写后，把打卡事件投递到 Kafka；
 * 消费端异步完成累计时长统计、缓存刷新、后续落库 / 消息通知等扩展动作，
 * 从而把“在线打卡主链路”和“后台聚合链路”解耦。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyCheckinEvent {

    /** 事件唯一标识（便于链路追踪 / 幂等去重） */
    private String eventId;

    /** 打卡用户 ID */
    private Integer userId;

    /** 本次学习时长（分钟） */
    private Integer minutes;

    /** 学习科目（可选，如：数学 / 英语 / 政治 / 专业课） */
    private String subject;

    /** 打卡日期，格式 yyyy-MM-dd */
    private String date;

    /** 打卡时的连续打卡天数 */
    private Integer streak;

    /** 事件产生时间，ISO-8601 字符串 */
    private String eventTime;
}
