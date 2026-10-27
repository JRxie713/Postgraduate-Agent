package com.demo.scaffoldbackend.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 智能组卷 / 薄弱点专项卷响应体
 */
@Data
public class AiPaperVO {

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

    /** 题目数量 */
    private Integer questionCount;

    /** 题目列表 */
    private List<Question> questions;

    /** 错题来源标记 */
    private Integer fromWrongBook;

    /** 薄弱知识点 */
    private List<String> weakPoints;

    /**
     * 组卷题目
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Question extends ExamQuestionVO {

        /** 题干 */
        private String questionText;

        /** 选项信息 */
        private String optionInfo;

        /** 参考答案 */
        private String refAnswer;

        /** 解析 */
        private String analysis;
    }
}
