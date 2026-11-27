package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 中英文自我介绍点评结果
 */
@Data
public class EnglishReviewVO {

    /** 批改记录ID(仅保存后返回) */
    private Integer id;

    /** 语言: zh(中文)/en(英文) */
    private String language;

    /** 学生提交的自我介绍原文 */
    private String introduction;

    /** 批改时间 */
    private LocalDateTime createTime;

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
