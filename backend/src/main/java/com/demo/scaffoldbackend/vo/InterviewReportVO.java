package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 复试能力报告响应体
 */
@Data
public class InterviewReportVO {

    /** 用户ID */
    private Integer userId;

    /** 专业 */
    private String major;

    /** 练习场次 */
    private Integer sessionCount;

    /** 平均分 */
    private Double avgScore;

    /** 得分趋势 */
    private List<Trend> trend;

    /** 薄弱项 */
    private List<String> weakness;

    /** 改进建议 */
    private List<String> suggestions;

    /**
     * 得分趋势项
     */
    @Data
    public static class Trend {

        /** 日期 */
        private String date;

        /** 当次得分 */
        private Double score;
    }
}
