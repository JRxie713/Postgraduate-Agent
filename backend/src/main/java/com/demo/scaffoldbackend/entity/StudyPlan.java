package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表4 学习计划表
 */
@Data
public class StudyPlan {

    /** 主键 */
    private Integer id;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
