package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识点掌握图谱响应体
 */
@Data
public class KnowledgeMapVO {

    /** 用户ID */
    private Integer userId;

    /** 考试科目 */
    private String examSubject;

    /** 整体掌握度 */
    private Double overallMastery;

    /** 章节列表 */
    private List<Chapter> chapters;

    /**
     * 章节节点
     */
    @Data
    public static class Chapter {

        /** 章节名称 */
        private String chapter;

        /** 章节掌握度 */
        private Double mastery;

        /** 题目数量 */
        private Integer questionCount;

        /** 知识点节点 */
        private List<Point> knowledgePoints;
    }

    /**
     * 知识点节点
     */
    @Data
    public static class Point {

        /** 知识点名称 */
        private String name;

        /** 掌握度 */
        private Double mastery;

        /** 掌握等级 */
        private String level;

        /** 错误次数 */
        private Integer wrongCount;
    }
}
