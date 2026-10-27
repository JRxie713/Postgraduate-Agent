package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 学情预警与干预建议响应体
 */
@Data
public class InterveneVO {

    /** 用户ID */
    private Integer userId;

    /** 预警等级 */
    private String alertLevel;

    /** 预警信号 */
    private List<Signal> signals;

    /** 干预动作 */
    private List<Intervention> interventions;

    /**
     * 预警信号
     */
    @Data
    public static class Signal {

        /** 信号类型 */
        private String type;

        /** 信号值 */
        private String value;

        /** 权重 */
        private Double weight;
    }

    /**
     * 干预动作
     */
    @Data
    public static class Intervention {

        /** 动作 */
        private String action;

        /** 说明 */
        private String desc;

        /** 是否自动执行 */
        private Boolean auto;
    }
}
