package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 薄弱点专项卷请求体
 */
@Data
public class WeakPaperDTO {

    /** 用户ID */
    private Integer userId;

    /** 考试科目 */
    private String examSubject;

    /** 题目数量 */
    private Integer questionCount;

    /** 薄弱知识点数量 */
    private Integer topWeakPoints;

    /** 是否包含错题 */
    private Boolean includeWrongQuestions;
}
