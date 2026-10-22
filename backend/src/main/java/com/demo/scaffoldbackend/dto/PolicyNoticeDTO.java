package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政策通知新增/更新请求体
 */
@Data
public class PolicyNoticeDTO {

    /** 通知标题 */
    private String title;

    /** 通知类型 */
    private String noticeType;

    /** 信息来源 */
    private String noticeSource;

    /** 生效时间 */
    private LocalDateTime effectTime;

    /** 标签 */
    private String subTags;
}
