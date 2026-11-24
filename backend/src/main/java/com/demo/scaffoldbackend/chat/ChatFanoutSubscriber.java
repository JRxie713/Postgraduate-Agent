package com.demo.scaffoldbackend.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 订阅 Redis 广播频道，把事件扇出到本实例维护的 STOMP 连接
 */
@Component
public class ChatFanoutSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ChatFanoutSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatFanoutSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatFanoutEvent event = objectMapper.readValue(body, ChatFanoutEvent.class);
            if ("SESSION".equals(event.getKind()) && event.getSessionId() != null) {
                messagingTemplate.convertAndSend(
                        ChatDestinations.SESSION_TOPIC_PREFIX + event.getSessionId(), event.getPayload());
            } else if ("USER".equals(event.getKind()) && event.getUserId() != null) {
                String queue = event.getQueue() == null ? "inbox" : event.getQueue();
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(event.getUserId()), "/queue/" + queue, event.getPayload());
            }
        } catch (Exception e) {
            // 单条广播失败不能影响整个订阅容器
            log.warn("聊天广播消息处理失败", e);
        }
    }
}
