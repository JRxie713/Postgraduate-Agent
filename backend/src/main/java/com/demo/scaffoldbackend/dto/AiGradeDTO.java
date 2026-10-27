package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 主观题 AI 批改请求体
 */
@Data
public class AiGradeDTO {

    /** 用户ID */
    private Integer userId;

    /** 模考ID */
    private Integer mockExamId;

    /** 试卷ID */
    private Integer paperId;

    /** 题目ID */
    private Integer questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 批改严格度：NORMAL / STRICT */
    private String strict;
}
