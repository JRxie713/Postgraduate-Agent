package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 计划自适应调整响应体
 */
@Data
public class PlanAdjustVO {

    /** 计划ID */
    private Integer planId;

    /** 采用策略 */
    private String strategy;

    /** 调整说明 */
    private String adjustSummary;

    /** 变更任务列表 */
    private List<ChangedTask> changedTasks;

    /** 调整后完成率 */
    private Double completionRate;

    /**
     * 变更任务
     */
    @Data
    public static class ChangedTask {

        /** 任务ID */
        private Integer taskId;

        /** 变更动作 */
        private String action;

        /** 调整前 */
        private String before;

        /** 调整后 */
        private String after;
    }
}
