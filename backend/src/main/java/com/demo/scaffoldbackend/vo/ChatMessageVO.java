package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Data
public class ChatMessageVO {

    private Long id;

    private Long sessionId;

    private Integer senderId;

    private String senderName;

    private String senderRole;

    private String clientMsgId;

    /** TEXT / IMAGE / FILE */
    private String msgType;

    private String content;

    private Long fileId;

    private String fileName;

    private Long fileSize;

    private String contentType;

    /** NORMAL / RECALLED */
    private String status;

    private LocalDateTime createTime;
}
