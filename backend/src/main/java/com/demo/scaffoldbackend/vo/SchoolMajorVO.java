package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 院校专业响应体
 */
@Data
public class SchoolMajorVO {

    /** 主键 */
    private Integer id;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
