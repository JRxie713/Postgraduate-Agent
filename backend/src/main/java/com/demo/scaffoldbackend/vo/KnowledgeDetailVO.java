package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 知识点详情响应体
 */
@Data
public class KnowledgeDetailVO {

    /** 知识点 */
    private String knowledgePoint;

    /** 考试科目 */
    private String examSubject;

    /** 所属章节 */
    private String chapter;

    /** 题目数量 */
    private Integer questionCount;

    /** 难度分布 */
    private Map<String, Integer> difficultyDist;

    /** 个人掌握度 */
    private Double personalMastery;

    /** 错误次数 */
    private Integer wrongCount;

    /** 相关题目ID */
    private List<Integer> relatedQuestions;
}
