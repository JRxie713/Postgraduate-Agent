package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * WebSocket 撤回消息入参
 */
@Data
public class ChatRecallDTO {

    private Long sessionId;

    private Long messageId;
}
