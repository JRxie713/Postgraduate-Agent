package com.demo.scaffoldbackend.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 院校热度事件 Kafka 消费者
 *
 * <p>异步处理生产者投递的热度事件，典型职责：</p>
 * <ul>
 *     <li>累计院校历史浏览量、关注量与总热度（Redis String 计数器）；</li>
 *     <li>预留扩展点：热度落库、生成择校推荐、推送热门院校动态等都可在这一侧追加。</li>
 * </ul>
 */
@Component
public class SchoolHeatKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SchoolHeatKafkaConsumer.class);

    /** 关注在总热度中的权重：一次关注顶 3 次浏览 */
    private static final int FOLLOW_WEIGHT = 3;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SchoolHeatKafkaConsumer(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 监听院校热度主题
     *
     * <p>消费组复用 application.yml 的 spring.kafka.consumer.group-id；
     * 同一消费组内每条消息只被一个实例消费。</p>
     */
    @KafkaListener(topics = SchoolHeatKafkaProducer.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onHeatEvent(String payload) {
        try {
            SchoolHeatEvent event = objectMapper.readValue(payload, SchoolHeatEvent.class);
            String name = event.getSchoolName();
            String action = event.getAction() == null ? "" : event.getAction().toUpperCase();

            // 历史累计浏览 / 关注 / 总热度计数器（长期保留，不设过期）
            switch (action) {
                case "FOLLOW" -> {
                    redis.opsForValue().increment(totalKey(name), FOLLOW_WEIGHT);
                    redis.opsForValue().increment(followKey(name), 1);
                }
                case "UNFOLLOW" -> {
                    redis.opsForValue().increment(totalKey(name), -FOLLOW_WEIGHT);
                    clampDecrement(followKey(name));
                }
                default -> {
                    redis.opsForValue().increment(totalKey(name), 1);
                    redis.opsForValue().increment(viewKey(name), 1);
                }
            }

            log.info("院校热度事件异步处理完成：school={}, action={}", name, event.getAction());
        } catch (Exception e) {
            log.error("院校热度事件处理失败，消息体={}, 原因={}", payload, e.getMessage());
            // 异常不向上抛出：避免消费位点反复重放导致毒消息；需要严格不丢时可写入死信主题
        }
    }

    /** 关注量计数器回退 1，最低不低于 0 */
    private void clampDecrement(String key) {
        Long current = redis.opsForValue().increment(key, -1);
        if (current != null && current < 0) {
            redis.opsForValue().set(key, "0");
        }
    }

    private String totalKey(String schoolName) {
        return "postgraduate:heat:total:" + schoolName;
    }

    private String viewKey(String schoolName) {
        return "postgraduate:heat:views:" + schoolName;
    }

    private String followKey(String schoolName) {
        return "postgraduate:heat:follows:" + schoolName;
    }
}
