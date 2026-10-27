package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 择校热度榜响应体
 */
@Data
public class SchoolHotVO {

    /** 院校专业ID */
    private Integer schoolMajorId;

    /** 院校名称 */
    private String school;

    /** 专业名称 */
    private String major;

    /** 设为目标人数 */
    private Integer targetCount;

    /** 用户平均预估分 */
    private Double avgPredictScore;

    /** 分数线 */
    private Double scoreLine;

    /** 招生人数 */
    private Integer enrollNum;

    /** 报录比 */
    private Double ratio;
}
