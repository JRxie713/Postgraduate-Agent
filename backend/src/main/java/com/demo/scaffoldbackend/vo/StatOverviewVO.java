package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 平台运营概览响应体
 */
@Data
public class StatOverviewVO {

    /** 统计范围：TODAY / WEEK / MONTH */
    private String range;

    /** 总用户数 */
    private Integer totalUsers;

    /** 活跃用户 */
    private Integer activeUsers;

    /** 新增用户 */
    private Integer newUsers;

    /** 人均学习时长（分钟） */
    private Integer avgStudyMinutes;

    /** 打卡率 */
    private Double checkInRate;

    /** 模考次数 */
    private Integer mockExamCount;

    /** 题目总量 */
    private Integer questionCount;

    /** 趋势 */
    private List<Trend> trend;

    /**
     * 趋势项
     */
    @Data
    public static class Trend {

        /** 日期 */
        private String date;

        /** 活跃用户 */
        private Integer activeUsers;

        /** 模考次数 */
        private Integer mockExamCount;
    }
}
