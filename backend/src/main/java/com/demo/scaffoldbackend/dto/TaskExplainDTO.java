package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 任务学习指引请求体
 */
@Data
public class TaskExplainDTO {

    /** 任务ID */
    private Integer taskId;

    /** 用户ID */
    private Integer userId;

    /** 讲解详细程度 */
    private String detailLevel;
}
