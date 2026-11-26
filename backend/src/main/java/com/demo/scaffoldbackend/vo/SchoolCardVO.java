package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 院校热度卡片响应体
 *
 * <p>用于择校面板展示：院校名称、关注人数、当前用户是否已关注，以及浏览量 / 热度兜底数据。</p>
 */
@Data
public class SchoolCardVO {

    /** 院校专业记录 ID */
    private Integer schoolId;

    /** 院校名称 */
    private String schoolName;

    /** 关注人数（关注该院校的去重用户数） */
    private Long follows;

    /** 当前用户是否已关注该院校 */
    private Boolean followed;

    /** 累计浏览量（消费端历史计数器兜底） */
    private Long views;

    /** 总热度（浏览 + 加权关注） */
    private Double heat;

    public SchoolCardVO(Integer schoolId, String schoolName, Long follows, Boolean followed,
                        Long views, Double heat) {
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.follows = follows;
        this.followed = followed;
        this.views = views;
        this.heat = heat;
    }
}
