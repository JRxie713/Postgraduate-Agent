package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 智能组卷请求体
 */
@Data
public class AiPaperGenerateDTO {

    /** 用户ID */
    private Integer userId;

    /** 试卷名称 */
    private String paperName;

    /** 考试科目 */
    private String examSubject;

    /** 试卷总分 */
    private Integer totalScore;

    /** 题目数量 */
    private Integer questionCount;

    /** 章节范围 */
    private List<String> chapters;

    /** 知识点范围 */
    private List<String> knowledgePoints;

    /** 题型范围 */
    private List<String> qTypes;

    /** 难度分布 */
    private Map<String, Double> difficultyDist;

    /** 排除最近天数内做过的题 */
    private Integer excludeRecentDays;

    /** 是否保存为试卷 */
    private Boolean saveAsPaper;
}
