package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表8 模考记录表
 */
@Data
public class MockExam {

    /** 主键 */
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 试卷ID */
    private Integer paperId;

    /** 开考时间 */
    private LocalDateTime startTime;

    /** 交卷时间 */
    private LocalDateTime submitTime;

    /** 本次得分 */
    private Double score;

    /** 排名 */
    private Integer examRank;

    /** 估分区间 */
    private String estimateRange;

    /** 考试状态 */
    private String examStatus;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
