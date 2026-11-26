package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * WebSocket 连接票据
 */
@Data
public class ChatWsTicketVO {

    private String ticket;

    private int expiresIn;

    private String wsPath;
}
