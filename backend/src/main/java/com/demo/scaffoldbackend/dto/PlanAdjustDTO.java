package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 计划自适应调整请求体
 */
@Data
public class PlanAdjustDTO {

    /** 计划ID */
    private Integer planId;

    /** 用户ID */
    private Integer userId;

    /** 调整原因 */
    private String reason;

    /** 原因描述 */
    private String reasonDesc;

    /** 调整策略 */
    private String strategy;
}
