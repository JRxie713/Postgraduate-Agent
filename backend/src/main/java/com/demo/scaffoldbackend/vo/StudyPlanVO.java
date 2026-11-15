package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习计划响应体
 */
@Data
public class StudyPlanVO {

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

    /** 计划下的任务列表 */
    private List<PlanTaskVO> tasks;

    /** 任务总数 */
    private Integer taskTotal;

    /** 已完成任务数 */
    private Integer taskDone;
}
