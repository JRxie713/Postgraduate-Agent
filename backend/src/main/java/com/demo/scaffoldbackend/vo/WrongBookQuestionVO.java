package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本条目（带题目详情）
 */
@Data
public class WrongBookQuestionVO {

    /** 错题记录ID */
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 题目ID */
    private Integer questionId;

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

    /** 题干 */
    private String questionText;

    /** 选项列表 */
    private List<QuestionOptionVO> options;

    /** 正确答案 */
    private String refAnswer;

    /** 解析 */
    private String analysis;

    /** 错误原因 */
    private String wrongReason;

    /** 掌握程度 */
    private String masteryLevel;

    /** 复习次数 */
    private Integer reviewCount;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 最近一次作答（错误）答案 */
    private String lastUserAnswer;

    /** 最近一次作答时间 */
    private LocalDateTime lastAnswerTime;

    /** 加入错题时间 */
    private LocalDateTime addTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
