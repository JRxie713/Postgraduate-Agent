package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计划任务新增/更新请求体
 */
@Data
public class PlanTaskDTO {

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
}
