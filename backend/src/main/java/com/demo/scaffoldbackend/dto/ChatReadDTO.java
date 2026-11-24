package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 已读上报入参
 */
@Data
public class ChatReadDTO {

    /** WebSocket 上报时使用；REST 接口从路径读取 */
    private Long sessionId;

    /** 客户端当前读到的最后一条消息ID */
    private Long lastReadMsgId;
}
