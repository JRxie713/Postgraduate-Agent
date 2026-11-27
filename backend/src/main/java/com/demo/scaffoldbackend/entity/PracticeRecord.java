package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日常练习行为记录表
 *
 * <p>只记录练习行为，题目内容仍复用 question_content / exam_question。</p>
 */
@Data
public class PracticeRecord {

    /** 主键 */
    private Long id;

    /** 用户ID */
    private Integer userId;

    /** 题目ID（exam_question.id） */
    private Integer questionId;

    /** 题目内容ID（question_content.id） */
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

    /** 用户答案 */
    private String userAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 是否正确 */
    private Boolean isCorrect;

    /** 答题时间 */
    private LocalDateTime answerTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
