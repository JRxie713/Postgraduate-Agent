package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 练习题响应体（练习过程中不返回答案与解析，避免提前泄题）
 */
@Data
public class PracticeQuestionVO {

    /** 题目ID（exam_question.id） */
    private Integer id;

    /** 题目内容ID */
    private Integer contentId;

    /** 考试科目 */
    private String examSubject;

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

    /** 选项列表 */
    private List<QuestionOptionVO> options;

    /** 当前用户是否已在错题本中 */
    private Boolean wrong;
}
