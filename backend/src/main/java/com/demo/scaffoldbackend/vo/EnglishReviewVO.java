package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 中英文自我介绍点评结果
 */
@Data
public class EnglishReviewVO {

    /** 综合评分(0-100) */
    private Double totalScore;

    /** 词汇丰富度(0-100) */
    private Double vocabularyScore;

    /** 语法准确度(0-100) */
    private Double grammarScore;

    /** 结构清晰度(0-100) */
    private Double structureScore;

    /** 总体评价 */
    private String overallComment;

    /** 具体修改建议 */
    private List<String> suggestions;

    /** 语法错误标记(规则化兜底用) */
    private List<String> grammarMistakes;

    /** 是否 AI 真实点评(1=是,0=规则化兜底) */
    private Integer ruleBased;
}
