package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 客观题作答结果
 */
@Data
public class PracticeAnswerResultVO {

    /** 题目ID */
    private Integer questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 是否答对 */
    private Boolean correct;

    /** 解析 */
    private String analysis;

    /** 题干 */
    private String questionText;

    /** 错题本记录ID（答错时自动收录后返回） */
    private Integer wrongQuestionId;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 当前掌握程度 */
    private String masteryLevel;

    /** 是否本次自动加入错题本 */
    private Boolean newlyAdded;
}
