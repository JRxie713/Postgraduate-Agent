package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答疑响应体
 */
@Data
public class ConsultationVO {

    /** 主键 */
    private Integer id;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
