package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 消息撤回
 */
@Data
public class ChatRecallVO {

    private Long sessionId;

    private Long messageId;

    private Integer operatorId;
}
