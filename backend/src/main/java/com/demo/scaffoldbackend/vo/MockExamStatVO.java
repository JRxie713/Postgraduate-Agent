package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 模考成绩分布响应体
 */
@Data
public class MockExamStatVO {

    /** 试卷ID */
    private Integer paperId;

    /** 试卷名称 */
    private String paperName;

    /** 参考人数 */
    private Integer participants;

    /** 平均分 */
    private Double avgScore;

    /** 最高分 */
    private Double maxScore;

    /** 中位数 */
    private Double medianScore;

    /** 分数段分布 */
    private List<ScoreBand> scoreBands;

    /**
     * 分数段
     */
    @Data
    public static class ScoreBand {

        /** 分数段 */
        private String band;

        /** 人数 */
        private Integer count;

        /** 占比 */
        private Double ratio;
    }
}
