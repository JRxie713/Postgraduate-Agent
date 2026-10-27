package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 陪学对话请求体
 */
@Data
public class MentorChatDTO {

    /** 用户ID */
    private Integer userId;

    /** 会话ID */
    private String sessionId;

    /** 用户消息 */
    private String message;

    /** 情绪标签 */
    private String emotionTag;
}
