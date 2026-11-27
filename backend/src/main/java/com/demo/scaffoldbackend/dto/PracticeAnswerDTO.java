package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 客观题作答请求体
 */
@Data
public class PracticeAnswerDTO {

    /** 题目ID（exam_question.id） */
    private Integer questionId;

    /** 用户答案，如 A / AB / 选项文本 */
    private String userAnswer;
}
