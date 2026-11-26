package com.demo.scaffoldbackend.kafka;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 院校热度事件 Kafka 生产者
 *
 * <p>只负责把事件 JSON 投递到主题，不关心消费端如何聚合；
 * 投递失败不回滚 Redis 中的实时计数，事件可由补偿任务重放。</p>
 */
@Component
public class SchoolHeatKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(SchoolHeatKafkaProducer.class);

    /** 院校热度事件主题（与 KafkaTopicConfig 中注册的主题保持一致） */
    public static final String TOPIC = "school-heat-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SchoolHeatKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送院校热度事件
     *
     * @param event 热度事件
     */
    public void sendHeatEvent(SchoolHeatEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // key 用院校名称：同一所院校的事件落到同一分区，保证单校事件顺序
            kafkaTemplate.send(TOPIC, event.getSchoolName(), payload);
            log.info("院校热度事件已投递 Kafka：school={}, action={}, topic={}",
                    event.getSchoolName(), event.getAction(), TOPIC);
        } catch (Exception e) {
            log.error("院校热度事件投递 Kafka 失败：school={}, 原因={}", event.getSchoolName(), e.getMessage());
        }
    }
}
