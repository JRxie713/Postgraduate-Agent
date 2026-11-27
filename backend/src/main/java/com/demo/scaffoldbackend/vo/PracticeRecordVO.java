package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习记录响应体
 */
@Data
public class PracticeRecordVO {

    /** 记录ID */
    private Long id;

    /** 题目ID */
    private Integer questionId;

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

    /** 用户答案 */
    private String userAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 是否答对 */
    private Boolean correct;

    /** 答题时间 */
    private LocalDateTime answerTime;
}
