package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自我介绍批改记录（中英文，供历史记录回看）
 */
@Data
public class InterviewEnglishReview {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 语言: zh(中文)/en(英文) */
    private String language;

    /** 学生提交的自我介绍原文 */
    private String introduction;

    /** 语言表达 / 词汇丰富度(0-100) */
    private Double vocabularyScore;

    /** 语法规范(0-100) */
    private Double grammarScore;

    /** 结构清晰度(0-100) */
    private Double structureScore;

    /** 加权总分(0-100) */
    private Double totalScore;

    /** 总体评价 */
    private String overallComment;

    /** 改进建议(JSON数组或换行文本) */
    private String suggestions;

    /** 表达问题(JSON数组或换行文本) */
    private String grammarMistakes;

    /** 是否规则化兜底(1=是) */
    private Integer ruleBased;

    /** 批改时间 */
    private LocalDateTime createTime;
}
