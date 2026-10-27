package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 学情周报响应体
 */
@Data
public class WeeklyReportVO {

    /** 用户ID */
    private Integer userId;

    /** 周区间 */
    private String weekRange;

    /** 学习时长（分钟） */
    private Integer studyMinutes;

    /** 打卡天数 */
    private Integer checkInDays;

    /** 平均专注度 */
    private Double avgFocus;

    /** 任务完成率 */
    private Double taskCompletionRate;

    /** 超时任务数 */
    private Integer timeoutTaskCount;

    /** 模考趋势 */
    private List<Trend> mockExamTrend;

    /** 错题净增 */
    private Integer wrongQuestionDelta;

    /** 情绪趋势 */
    private List<String> emotionTrend;

    /** 本周亮点 */
    private List<String> highlights;

    /** 风险提示 */
    private List<String> risks;

    /** 下周重点 */
    private List<String> nextWeekFocus;

    /**
     * 模考趋势项
     */
    @Data
    public static class Trend {

        /** 日期 */
        private String date;

        /** 分数 */
        private Double score;
    }
}
