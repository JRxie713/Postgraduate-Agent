package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 入学诊断卷响应体
 */
@Data
public class AiDiagnosePaperVO {

    /** 试卷ID */
    private Integer paperId;

    /** 试卷名称 */
    private String paperName;

    /** 考试科目 */
    private String examSubject;

    /** 试卷总分 */
    private Integer totalScore;

    /** 试卷类型 */
    private Integer paperType;

    /** 题目列表 */
    private List<Question> questions;

    /**
     * 诊断题目
     */
    @Data
    public static class Question {

        /** 题目ID */
        private Integer questionId;

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

        /** 题干 */
        private String questionText;

        /** 选项信息 */
        private String optionInfo;
    }
}
