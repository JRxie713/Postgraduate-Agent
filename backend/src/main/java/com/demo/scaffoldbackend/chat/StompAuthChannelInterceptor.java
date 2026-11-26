package com.demo.scaffoldbackend.chat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.scaffoldbackend.entity.ChatSessionMember;
import com.demo.scaffoldbackend.mapper.ChatSessionMemberMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP 入站校验：
 * 1. CONNECT 必须带有握手阶段解析出的用户主体；
 * 2. SUBSCRIBE 只能订阅自己所在会话的 /topic/session/{id} 或自己的 /user/queue/**；
 * 3. SEND 只能发往 /app/chat.* 业务端点。
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final ChatSessionMemberMapper memberMapper;

    public StompAuthChannelInterceptor(ChatSessionMemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        Principal principal = accessor.getUser();

        if (StompCommand.CONNECT.equals(command)) {
            if (principal == null) {
                throw new MessagingException("未认证的 WebSocket 连接");
            }
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            if (principal == null) {
                throw new MessagingException("未认证的 WebSocket 订阅");
            }
            checkSubscribe(accessor.getDestination(), principal);
            return message;
        }

        if (StompCommand.SEND.equals(command)) {
            if (principal == null) {
                throw new MessagingException("未认证的 WebSocket 请求");
            }
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith(ChatDestinations.APP_PREFIX + "/chat.")) {
                throw new MessagingException("非法的发送目的地：" + destination);
            }
        }
        return message;
    }

    private void checkSubscribe(String destination, Principal principal) {
        if (destination == null) {
            throw new MessagingException("订阅目的地不能为空");
        }
        if (destination.startsWith("/user/queue/")) {
            return;
        }
        String prefix = ChatDestinations.SESSION_TOPIC_PREFIX;
        if (!destination.startsWith(prefix)) {
            throw new MessagingException("非法订阅目的地：" + destination);
        }
        Long sessionId = parseSessionId(destination.substring(prefix.length()));
        if (sessionId == null) {
            throw new MessagingException("非法会话ID");
        }
        Integer userId;
        try {
            userId = Integer.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new MessagingException("非法用户身份");
        }
        Long count = memberMapper.selectCount(Wrappers.<ChatSessionMember>lambdaQuery()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId));
        if (count == null || count == 0) {
            throw new MessagingException("无权订阅该会话");
        }
    }

    private Long parseSessionId(String raw) {
        String value = raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
        if (value.isEmpty() || value.contains("/")) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
