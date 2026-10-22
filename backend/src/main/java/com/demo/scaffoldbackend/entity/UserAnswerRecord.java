package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户答题记录表
 */
@Data
public class UserAnswerRecord {

    /** 主键 */
    private Integer id;

    /** 模考ID */
    private Integer mockExamId;

    /** 用户ID */
    private Integer userId;

    /** 试卷ID */
    private Integer paperId;

    /** 题目ID */
    private Integer questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 是否正确 */
    private Boolean isCorrect;

    /** 本题得分 */
    private Double scoreObtain;

    /** 答题时间 */
    private LocalDateTime answerTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
