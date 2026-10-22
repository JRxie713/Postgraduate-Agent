package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * AI 答疑请求体（提问 / 追问 / 会话总结 / 拍照答疑共用）
 */
@Data
public class AiQaDTO {

    /** 用户ID */
    private Integer userId;

    /** 会话ID */
    private String sessionId;

    /** 提问内容 */
    private String question;

    /** 考试科目 */
    private String examSubject;

    /** 关联题目ID */
    private Integer questionId;

    /** 上下文来源 */
    private String contextFrom;

    /** 图片地址（拍照答疑） */
    private String imageUrl;
}
