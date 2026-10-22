package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政策通知响应体
 */
@Data
public class PolicyNoticeVO {

    /** 主键 */
    private Integer id;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
