package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 错题新增/更新请求体
 */
@Data
public class WrongQuestionDTO {

    /** 用户ID */
    private Integer userId;

    /** 题目ID */
    private Integer questionId;

    /** 错误原因 */
    private String wrongReason;

    /** 掌握程度 */
    private String masteryLevel;
}
