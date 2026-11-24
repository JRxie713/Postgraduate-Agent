package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Integer senderId;

    /** 客户端消息ID，用于发送幂等 */
    private String clientMsgId;

    /** TEXT / IMAGE / FILE */
    private String msgType;

    private String content;

    private Long fileId;

    /** NORMAL / RECALLED */
    private String status;

    private LocalDateTime recallTime;

    private LocalDateTime createTime;
}
