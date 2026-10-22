package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 计划任务状态更新请求体
 */
@Data
public class PlanTaskStatusDTO {

    /** 任务状态 */
    private String taskStatus;
}
