package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷模板表
 */
@Data
public class ExamPaper {

    /** 主键 */
    private Integer id;

    /** 试卷名 */
    private String paperName;

    /** 考试科目 */
    private String examSubject;

    /** 试卷总分 */
    private Integer totalScore;

    /** 试卷类型 */
    private Integer paperType;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
