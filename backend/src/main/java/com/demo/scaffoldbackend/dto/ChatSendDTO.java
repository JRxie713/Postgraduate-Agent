package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * WebSocket 发送消息入参
 */
@Data
public class ChatSendDTO {

    private Long sessionId;

    /** 客户端生成的消息ID，用于幂等去重 */
    private String clientMsgId;

    /** TEXT / IMAGE / FILE */
    private String msgType;

    /** 文本内容 */
    private String content;

    /** 附件ID（IMAGE / FILE 时必填） */
    private Long fileId;
}
