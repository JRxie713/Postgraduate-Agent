package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习计划新增/更新请求体
 */
@Data
public class StudyPlanDTO {

    /** 用户ID */
    private Integer userId;

    /** 计划周期 */
    private String planCycle;

    /** 计划开始时间 */
    private LocalDateTime startDate;

    /** 计划结束时间 */
    private LocalDateTime endDate;

    /** 计划状态 */
    private String planStatus;

    /** 完成率 */
    private Double completionRate;
}
