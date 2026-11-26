package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 正在输入
 */
@Data
public class ChatTypingVO {

    private Long sessionId;

    private Integer userId;

    private String name;
}
