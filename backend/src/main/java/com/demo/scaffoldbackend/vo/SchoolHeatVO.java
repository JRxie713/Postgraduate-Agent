package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 院校热度榜条目响应体
 */
@Data
public class SchoolHeatVO {

    /** 名次，从 1 开始 */
    private Long rank;

    /** 院校专业记录 ID */
    private Integer schoolId;

    /** 院校名称 */
    private String schoolName;

    /** 总热度（浏览 + 加权关注） */
    private Double heat;

    /** 累计浏览量 */
    private Long views;

    /** 累计关注量 */
    private Long follows;
}
