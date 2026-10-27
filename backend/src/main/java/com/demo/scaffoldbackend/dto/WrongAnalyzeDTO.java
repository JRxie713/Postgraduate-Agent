package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 错题归因分析请求体
 */
@Data
public class WrongAnalyzeDTO {

    /** 错题ID */
    private Integer wrongQuestionId;

    /** 用户ID */
    private Integer userId;

    /** 用户答案 */
    private String userAnswer;

    /** 自述错因 */
    private String selfReason;
}
