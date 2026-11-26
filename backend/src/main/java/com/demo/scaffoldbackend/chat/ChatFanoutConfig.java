package com.demo.scaffoldbackend.chat;

import com.demo.scaffoldbackend.config.ChatProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 订阅容器：多实例聊天广播
 */
@Configuration
public class ChatFanoutConfig {

    @Bean
    public RedisMessageListenerContainer chatFanoutListenerContainer(RedisConnectionFactory connectionFactory,
                                                                     ChatFanoutSubscriber subscriber,
                                                                     ChatProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(properties.getFanoutChannel()));
        return container;
    }
}
