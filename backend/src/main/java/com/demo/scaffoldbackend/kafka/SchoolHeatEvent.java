package com.demo.scaffoldbackend.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 院校热度事件（Kafka 消息体）
 *
 * <p>主流程在 Redis 完成当日去重与实时计数后，把热度事件投递到 Kafka；
 * 消费端异步累计该院校的历史浏览量、关注量与总热度，作为后续落库 / 推荐的扩展点。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolHeatEvent {

    /** 事件唯一标识（链路追踪 / 幂等去重） */
    private String eventId;

    /** 操作用户 ID */
    private Integer userId;

    /** 院校专业记录 ID */
    private Integer schoolId;

    /** 院校名称（热度聚合维度） */
    private String schoolName;

    /** 动作类型：VIEW 浏览 / FOLLOW 关注 / UNFOLLOW 取关 */
    private String action;

    /** 事件产生时间，ISO-8601 字符串 */
    private String eventTime;
}
