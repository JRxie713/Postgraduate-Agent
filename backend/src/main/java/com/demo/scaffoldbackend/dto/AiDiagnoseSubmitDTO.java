package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 入学诊断提交请求体
 */
@Data
public class AiDiagnoseSubmitDTO {

    /** 用户ID */
    private Integer userId;

    /** 试卷ID */
    private Integer paperId;

    /** 作答明细 */
    private List<AnswerItem> answers;

    /**
     * 单题作答
     */
    @Data
    public static class AnswerItem {

        /** 题目ID */
        private Integer questionId;

        /** 用户答案 */
        private String userAnswer;
    }
}
