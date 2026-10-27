package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 薄弱知识点响应体
 */
@Data
public class WeakPointVO {

    /** 用户ID */
    private Integer userId;

    /** 薄弱知识点总数 */
    private Integer totalWeakPoints;

    /** 薄弱知识点列表 */
    private List<Item> items;

    /**
     * 薄弱知识点项
     */
    @Data
    public static class Item {

        /** 知识点 */
        private String knowledgePoint;

        /** 所属科目 */
        private String examSubject;

        /** 掌握度 */
        private Double mastery;

        /** 掌握等级 */
        private String masteryLevel;

        /** 错误次数 */
        private Integer wrongCount;

        /** 趋势：UP / FLAT / DOWN */
        private String trend;
    }
}
