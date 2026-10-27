package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 督导提醒响应体
 */
@Data
public class MentorRemindVO {

    /** 用户ID */
    private Integer userId;

    /** 提醒类型 */
    private String remindType;

    /** 语气 */
    private String tone;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;
}
