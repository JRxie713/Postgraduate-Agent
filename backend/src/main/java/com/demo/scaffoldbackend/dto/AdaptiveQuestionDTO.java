package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 自适应出题请求体
 */
@Data
public class AdaptiveQuestionDTO {

    /** 用户ID */
    private Integer userId;

    /** 考试科目 */
    private String examSubject;

    /** 当前难度 */
    private String currentDifficulty;

    /** 知识点 */
    private String knowledgePoint;

    /** 已答数量 */
    private Integer answeredCount;

    /** 计划题量 */
    private Integer planCount;

    /** 排除题目ID */
    private List<Integer> excludeQuestionIds;
}
