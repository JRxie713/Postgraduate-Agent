package com.demo.scaffoldbackend.vo;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习记录详情（含完整题目，支持 Redis 缓存）
 */
@Data
public class PracticeRecordDetailVO {

    /** 记录ID（雪花/BigInt，序列化为 String 防 JS 精度丢失） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 题目ID */
    private Integer questionId;

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

    /** 我的答案 */
    private String userAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 是否答对 */
    private Boolean correct;

    /** 解析 */
    private String analysis;

    /** 答题时间 */
    private LocalDateTime answerTime;
}
