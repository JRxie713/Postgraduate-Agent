package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 答疑新增/更新请求体
 */
@Data
public class ConsultationDTO {

    /** 用户ID */
    private Integer userId;

    /** 一轮对话唯一 ID */
    private String sessionId;

    /** 用户提问 */
    private String question;

    /** 回复内容 */
    private String answer;

    /** 答疑来源 */
    private String consultSource;

    /** 引用资源id */
    private Integer refId;

    /** 追问轮次 */
    private Integer round;
}
