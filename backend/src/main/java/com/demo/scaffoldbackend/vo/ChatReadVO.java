package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 已读回执：群聊按已读游标聚合阅读人数
 */
@Data
public class ChatReadVO {

    private Long sessionId;

    private Integer userId;

    private Long lastReadMsgId;

    /** 群聊：已读人数（含发送者）；私聊不使用 */
    private Integer readCount;

    private Integer memberCount;
}
