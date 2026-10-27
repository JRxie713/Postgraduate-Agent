package com.demo.scaffoldbackend.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 冲稳保择校推荐响应体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SchoolRecommendVO extends SchoolMajorVO {

    /** 分差 */
    private Double gap;

    /** 录取概率 */
    private Double probability;

    /** 风险等级 */
    private String riskLevel;

    /** 冲稳保档位 */
    private String tier;

    /** 推荐理由 */
    private String reason;
}
