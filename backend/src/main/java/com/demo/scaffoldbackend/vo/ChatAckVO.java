package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 发送结果回执
 */
@Data
public class ChatAckVO {

    private String clientMsgId;

    private boolean ok;

    private String message;

    private Long messageId;
}
