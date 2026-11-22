package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 排行榜条目响应体
 */
@Data
public class StudyRankVO {

    /** 名次，从 1 开始 */
    private Long rank;

    /** 用户 ID */
    private String userId;

    /** 榜单分数：连续打卡天数 / 当日学习分钟数 */
    private Double score;
}
