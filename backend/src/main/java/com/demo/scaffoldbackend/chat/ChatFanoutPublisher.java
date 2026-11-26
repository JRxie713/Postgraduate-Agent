package com.demo.scaffoldbackend.chat;

import com.demo.scaffoldbackend.config.ChatProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 发布聊天事件到 Redis Pub/Sub。
 *
 * <p>业务代码不直接调用 SimpMessagingTemplate，统一走“发布 -> 各实例订阅 -> 本机扇出”，
 * 保证多实例部署时任意实例都能收到广播。</p>
 */
@Component
public class ChatFanoutPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ChatProperties properties;

    public ChatFanoutPublisher(StringRedisTemplate redis, ObjectMapper objectMapper, ChatProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** 广播到会话主题 */
    public void toSession(Long sessionId, Object payload) {
        ChatFanoutEvent event = new ChatFanoutEvent();
        event.setKind("SESSION");
        event.setSessionId(sessionId);
        event.setPayload(toJson(payload));
        publish(event);
    }

    /** 推送到指定用户的私有队列（ack / inbox） */
    public void toUser(Integer userId, String queue, Object payload) {
        ChatFanoutEvent event = new ChatFanoutEvent();
        event.setKind("USER");
        event.setUserId(userId);
        event.setQueue(queue);
        event.setPayload(toJson(payload));
        publish(event);
    }

    private void publish(ChatFanoutEvent event) {
        redis.convertAndSend(properties.getFanoutChannel(), toJson(event));
    }

    private String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
