package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.chat.ChatFanoutPublisher;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.ChatReadDTO;
import com.demo.scaffoldbackend.dto.ChatRecallDTO;
import com.demo.scaffoldbackend.dto.ChatSendDTO;
import com.demo.scaffoldbackend.dto.ChatTypingDTO;
import com.demo.scaffoldbackend.service.ChatService;
import com.demo.scaffoldbackend.vo.ChatAckVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;

/**
 * STOMP 业务入口：/app/chat.send、/app/chat.read、/app/chat.recall、/app/chat.typing
 *
 * <p>统一使用 String 载荷 + 后端 ObjectMapper 解析，规避 STOMP 消息转换器差异；
 * 失败时通过 /user/queue/ack 回执错误，不直接断开连接。</p>
 */
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private static final Logger log = LoggerFactory.getLogger(ChatWsController.class);

    private final ChatService chatService;
    private final ChatFanoutPublisher publisher;
    private final ObjectMapper objectMapper;

    @MessageMapping("/chat.send")
    public void send(Principal principal, @Payload String payload) {
        Integer userId = userId(principal);
        ChatSendDTO dto = null;
        try {
            dto = parse(payload, ChatSendDTO.class);
            chatService.send(userId, dto);
        } catch (BusinessException e) {
            publisher.toUser(userId, "ack", errorAck(dto == null ? null : dto.getClientMsgId(), e.getMessage()));
        } catch (Exception e) {
            log.warn("WebSocket 发送消息失败：userId={}", userId, e);
            publisher.toUser(userId, "ack", errorAck(dto == null ? null : dto.getClientMsgId(), "发送失败，请稍后重试"));
        }
    }

    @MessageMapping("/chat.read")
    public void read(Principal principal, @Payload String payload) {
        Integer userId = userId(principal);
        ChatReadDTO dto = null;
        try {
            dto = parse(payload, ChatReadDTO.class);
            chatService.markRead(userId, dto.getSessionId(), dto.getLastReadMsgId());
        } catch (BusinessException e) {
            // 已读上报失败不打扰用户，仅记录日志
            log.debug("已读上报失败：userId={}, reason={}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("WebSocket 已读上报失败：userId={}", userId, e);
        }
    }

    @MessageMapping("/chat.recall")
    public void recall(Principal principal, @Payload String payload) {
        Integer userId = userId(principal);
        ChatRecallDTO dto = null;
        try {
            dto = parse(payload, ChatRecallDTO.class);
            chatService.recall(userId, dto.getMessageId());
        } catch (BusinessException e) {
            publisher.toUser(userId, "ack", errorAck(null, e.getMessage()));
        } catch (Exception e) {
            log.warn("WebSocket 撤回消息失败：userId={}", userId, e);
            publisher.toUser(userId, "ack", errorAck(null, "撤回失败，请稍后重试"));
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(Principal principal, @Payload String payload) {
        Integer userId = userId(principal);
        try {
            ChatTypingDTO dto = parse(payload, ChatTypingDTO.class);
            chatService.typing(userId, dto.getSessionId());
        } catch (Exception e) {
            // 正在输入是弱提示，失败静默
            log.debug("正在输入广播失败：userId={}", userId);
        }
    }

    @MessageMapping("/chat.ping")
    public void ping(Principal principal) {
        ChatAckVO ack = new ChatAckVO();
        ack.setOk(true);
        ack.setMessage("pong");
        publisher.toUser(userId(principal), "ack", ack);
    }

    private Integer userId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(401, "未认证的 WebSocket 连接");
        }
        try {
            return Integer.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "非法用户身份");
        }
    }

    private <T> T parse(String payload, Class<T> type) {
        if (payload == null || payload.isBlank()) {
            throw new BusinessException(400, "请求内容不能为空");
        }
        return objectMapper.readValue(payload, type);
    }

    private ChatAckVO errorAck(String clientMsgId, String message) {
        ChatAckVO ack = new ChatAckVO();
        ack.setClientMsgId(clientMsgId);
        ack.setOk(false);
        ack.setMessage(message);
        return ack;
    }
}
