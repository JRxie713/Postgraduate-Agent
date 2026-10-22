package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 院校专业新增/更新请求体
 */
@Data
public class SchoolMajorDTO {

    /** 院校名称 */
    private String school;

    /** 专业名称 */
    private String major;

    /** 招生人数 */
    private Integer enrollNum;

    /** 报录比 */
    private Double ratio;

    /** 分数线 */
    private Double scoreLine;

    /** 考试科目 */
    private String examSubjects;

    /** 参考书目 */
    private String refBooks;
}
