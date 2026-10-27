package com.demo.scaffoldbackend.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 今日导学任务卡响应体
 */
@Data
public class TodayGuideVO {

    /** 用户ID */
    private Integer userId;

    /** 日期 */
    private String date;

    /** 任务总数 */
    private Integer taskCount;

    /** 已完成数 */
    private Integer doneCount;

    /** 超时数 */
    private Integer timeoutCount;

    /** 任务列表 */
    private List<Task> tasks;

    /** 错题复习提醒 */
    private ReviewRemind reviewRemind;

    /**
     * 今日任务
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Task extends PlanTaskVO {

        /** 建议顺序 */
        private Integer suggestOrder;
    }

    /**
     * 错题复习提醒
     */
    @Data
    public static class ReviewRemind {

        /** 待复习错题数 */
        private Integer wrongQuestionCount;

        /** 提醒文案 */
        private String message;
    }
}
