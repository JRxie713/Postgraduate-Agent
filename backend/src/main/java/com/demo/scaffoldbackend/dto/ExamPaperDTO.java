package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 试卷新增/更新请求体
 */
@Data
public class ExamPaperDTO {

    /** 试卷名 */
    private String paperName;

    /** 考试科目 */
    private String examSubject;

    /** 试卷总分 */
    private Integer totalScore;

    /** 试卷类型 */
    private Integer paperType;
}
