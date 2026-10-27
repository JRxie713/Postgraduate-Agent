package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开始模考请求体
 */
@Data
public class MockExamStartDTO {

    /** 用户ID */
    private Integer userId;

    /** 试卷ID */
    private Integer paperId;

    /** 开考时间 */
    private LocalDateTime startTime;
}
