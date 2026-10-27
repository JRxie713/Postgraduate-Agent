package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题记录新增/更新请求体
 */
@Data
public class UserAnswerRecordDTO {

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
}
