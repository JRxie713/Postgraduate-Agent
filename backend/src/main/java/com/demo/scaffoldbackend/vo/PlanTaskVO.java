package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计划任务响应体
 */
@Data
public class PlanTaskVO {

    /** 主键 */
    private Integer id;

    /** 计划ID */
    private Integer planId;

    /** 任务名称 */
    private String taskName;

    /** 考试科目 */
    private String taskSubject;

    /** 章节 */
    private String chapter;

    /** 截止时间 */
    private LocalDateTime dueDate;

    /** 任务状态 */
    private String taskStatus;

    /** 是否超时 */
    private Boolean isTimeout;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
