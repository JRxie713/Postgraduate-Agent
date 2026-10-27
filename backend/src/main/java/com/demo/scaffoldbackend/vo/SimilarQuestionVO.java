package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 相似题推荐响应体
 */
@Data
public class SimilarQuestionVO {

    /** 题目ID */
    private Integer questionId;

    /** 题目内容ID */
    private Integer contentId;

    /** 考试科目 */
    private String examSubject;

    /** 所属章节 */
    private String chapter;

    /** 知识点 */
    private String knowledgePoint;

    /** 题型 */
    private String qType;

    /** 难度等级 */
    private String difficulty;

    /** 题干 */
    private String questionText;

    /** 相似度 */
    private Double similarity;
}
