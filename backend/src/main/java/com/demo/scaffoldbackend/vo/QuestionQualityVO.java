package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 题库质量统计响应体
 */
@Data
public class QuestionQualityVO {

    /** 题目ID */
    private Integer questionId;

    /** 知识点 */
    private String knowledgePoint;

    /** 难度等级 */
    private String difficulty;

    /** 使用次数 */
    private Integer usageCount;

    /** 正确率 */
    private Double correctRate;

    /** 被标记次数 */
    private Integer flaggedCount;

    /** 是否有解析 */
    private Boolean hasAnalysis;

    /** 质量分 */
    private Double qualityScore;
}
