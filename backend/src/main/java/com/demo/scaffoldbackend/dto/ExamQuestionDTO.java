package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 题目新增/更新请求体
 */
@Data
public class ExamQuestionDTO {

    /** 试卷ID */
    private Integer paperId;

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

    /** 分值 */
    private Double score;
}
