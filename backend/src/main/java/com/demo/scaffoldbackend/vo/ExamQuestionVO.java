package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目响应体
 */
@Data
public class ExamQuestionVO {

    /** 主键 */
    private Integer id;

    /** 试卷ID */
    private Integer paperId;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
