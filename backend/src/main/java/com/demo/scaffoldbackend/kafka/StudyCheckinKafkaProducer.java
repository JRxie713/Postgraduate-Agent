package com.demo.scaffoldbackend.kafka;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 打卡事件 Kafka 生产者
 *
 * <p>只负责把事件 JSON 投递到主题，不关心消费端如何处理；
 * 消费失败 / 重试由 Kafka 消费者侧与 Broker 重试策略保证。</p>
 */
@Component
public class StudyCheckinKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(StudyCheckinKafkaProducer.class);

    /** 打卡事件主题（与 KafkaTopicConfig 中注册的主题保持一致） */
    public static final String TOPIC = "study-checkin-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public StudyCheckinKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送打卡事件
     *
     * @param event 打卡事件
     */
    public void sendCheckinEvent(StudyCheckinEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // key 用 userId：同一用户的事件落到同一分区，保证个人事件顺序
            kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), payload);
            log.info("打卡事件已投递 Kafka：userId={}, minutes={}, topic={}",
                    event.getUserId(), event.getMinutes(), TOPIC);
        } catch (Exception e) {
            // 投递失败不回滚打卡主流程：Redis 中的实时数据已落，事件可由补偿任务重放
            log.error("打卡事件投递 Kafka 失败：userId={}, 原因={}", event.getUserId(), e.getMessage());
        }
    }
}
