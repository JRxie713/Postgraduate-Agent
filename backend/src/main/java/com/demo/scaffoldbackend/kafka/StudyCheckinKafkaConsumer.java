package com.demo.scaffoldbackend.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 打卡事件 Kafka 消费者
 *
 * <p>异步处理生产者投递的打卡事件，典型职责：</p>
 * <ul>
 *     <li>累计用户历史总学习时长（Redis String 计数器）；</li>
 *     <li>失效个人概览缓存，等待下次查询时按最新数据重建；</li>
 *     <li>预留扩展点：落库、推送学习提醒、生成周报等都可以在这一侧追加。</li>
 * </ul>
 */
@Component
public class StudyCheckinKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(StudyCheckinKafkaConsumer.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public StudyCheckinKafkaConsumer(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 监听打卡主题
     *
     * <p>group-id 取自 application.yml 中的 spring.kafka.consumer.group-id；
     * 同一消费组内每条消息只被一个实例消费。</p>
     */
    @KafkaListener(topics = StudyCheckinKafkaProducer.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onCheckinEvent(String payload) {
        try {
            StudyCheckinEvent event = objectMapper.readValue(payload, StudyCheckinEvent.class);
            Integer userId = event.getUserId();

            // 场景一：String 计数器 —— 累计历史总学习分钟数（长期保留，不设过期）
            String totalKey = "study:total-minutes:" + userId;
            redis.opsForValue().increment(totalKey, event.getMinutes());

            // 场景二：缓存失效 —— 删除个人概览 Hash 缓存，下次查询时读穿透重建，避免脏数据
            redis.delete("study:overview:" + userId);

            log.info("打卡事件异步处理完成：userId={}, 本次={}分钟, 连续={}天, 日期={}",
                    userId, event.getMinutes(), event.getStreak(), event.getDate());
        } catch (Exception e) {
            log.error("打卡事件处理失败，消息体={}, 原因={}", payload, e.getMessage());
            // 异常不向上抛出：避免消费位点反复重放导致毒消息；需要严格不丢时可写入死信主题
        }
    }
}
