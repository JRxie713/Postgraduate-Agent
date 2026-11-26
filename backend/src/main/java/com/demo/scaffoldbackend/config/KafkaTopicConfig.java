package com.demo.scaffoldbackend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 主题注册配置
 *
 * <p>应用启动时自动创建打卡事件主题；若 Broker 已存在同名主题则保持不变。
 * 分区数 3 便于后续多消费实例并行处理，副本数 1 适配本地 / 单机 Docker 环境。</p>
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic studyCheckinTopic() {
        return TopicBuilder.name("study-checkin-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic schoolHeatTopic() {
        return TopicBuilder.name("school-heat-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
