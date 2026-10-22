package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 学情分布统计响应体
 */
@Data
public class StatDistributionVO {

    /** 分组维度 */
    private String groupBy;

    /** 总人数 */
    private Integer total;

    /** 分组明细 */
    private List<Item> items;

    /**
     * 分组项
     */
    @Data
    public static class Item {

        /** 分组键 */
        private String key;

        /** 显示名 */
        private String label;

        /** 人数 */
        private Integer count;

        /** 占比 */
        private Double ratio;

        /** 平均分 */
        private Double avgScore;
    }
}
