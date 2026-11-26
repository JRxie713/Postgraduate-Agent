package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 最终面试报告（五维评分 + 评语 + 建议）
 */
@Data
public class FinalInterviewReportVO {

    /** 报告ID */
    private Integer id;

    /** 会话ID */
    private Integer sessionId;

    /** 五维评分 */
    private Score score;

    /** 加权总分 */
    private Double totalScore;

    /** AI 综合总评 */
    private String summary;

    /** 主要问题列表 */
    private List<String> mainIssues;

    /** 改进建议列表 */
    private List<String> suggestions;

    /** 本次报告是否为规则化兜底 */
    private Integer ruleBased;

    /** 报告生成时间 */
    private String createTime;

    /**
     * 五维评分
     */
    @Data
    public static class Score {
        /** 表达能力 */
        private Double expression;
        /** 专业知识 */
        private Double professional;
        /** 逻辑思维 */
        private Double logic;
        /** 英语能力 */
        private Double english;
        /** 应变能力 */
        private Double adaptability;
    }
}
