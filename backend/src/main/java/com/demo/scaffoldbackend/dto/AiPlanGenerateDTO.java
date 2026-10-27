package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 生成学习计划请求体
 */
@Data
public class AiPlanGenerateDTO {

    /** 用户ID */
    private Integer userId;

    /** 计划周期 */
    private String planCycle;

    /** 计划开始日期 */
    private LocalDate startDate;

    /** 计划结束日期 */
    private LocalDate endDate;

    /** 每日学习时长（小时） */
    private Double dailyHours;

    /** 重点科目 */
    private List<String> focusSubjects;

    /** 是否自动创建任务 */
    private Boolean autoCreateTasks;
}
