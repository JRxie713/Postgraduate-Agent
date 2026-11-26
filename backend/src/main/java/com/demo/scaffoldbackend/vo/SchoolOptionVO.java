package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 可选院校下拉项响应体（ID + 院校名称）
 */
@Data
public class SchoolOptionVO {

    /** 院校专业记录 ID */
    private Integer schoolId;

    /** 院校名称 */
    private String schoolName;

    public SchoolOptionVO(Integer schoolId, String schoolName) {
        this.schoolId = schoolId;
        this.schoolName = schoolName;
    }
}
