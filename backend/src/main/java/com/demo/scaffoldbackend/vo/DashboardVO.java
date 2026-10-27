package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 学情驾驶舱响应体
 */
@Data
public class DashboardVO {

    /** 用户ID */
    private Integer userId;

    /** 备考阶段 */
    private String examStage;

    /** 距考试天数 */
    private Integer daysToExam;

    /** 备考进度 */
    private Double progress;

    /** 能力雷达 */
    private List<Ability> abilityRadar;

    /** 目标达成度 */
    private Target target;

    /** 今日概况 */
    private Today today;

    /** 打卡连续记录 */
    private Streak streak;

    /** 待办提醒 */
    private List<Todo> todo;

    /** 导师寄语 */
    private String mentorMessage;

    /**
     * 能力维度
     */
    @Data
    public static class Ability {

        /** 维度 */
        private String dim;

        /** 得分 */
        private Double score;
    }

    /**
     * 目标达成度
     */
    @Data
    public static class Target {

        /** 目标院校 */
        private String school;

        /** 目标专业 */
        private String major;

        /** 分数线 */
        private Double scoreLine;

        /** 预估分 */
        private Double predictScore;

        /** 差距 */
        private Double gap;
    }

    /**
     * 今日概况
     */
    @Data
    public static class Today {

        /** 任务总数 */
        private Integer taskCount;

        /** 已完成数 */
        private Integer doneCount;

        /** 超时数 */
        private Integer timeoutCount;
    }

    /**
     * 打卡连续记录
     */
    @Data
    public static class Streak {

        /** 连续打卡天数 */
        private Integer checkInDays;

        /** 最长记录 */
        private Integer bestRecord;
    }

    /**
     * 待办提醒
     */
    @Data
    public static class Todo {

        /** 待办类型 */
        private String type;

        /** 数量 */
        private Integer count;

        /** 文案 */
        private String text;
    }
}
