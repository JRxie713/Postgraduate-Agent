package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模考记录更新请求体
 */
@Data
public class MockExamDTO {

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
}
