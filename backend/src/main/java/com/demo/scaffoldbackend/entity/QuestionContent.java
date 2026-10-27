package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表6.1 题目内容表
 */
@Data
public class QuestionContent {

    /** 主键 */
    private Integer id;

    /** 题干 */
    private String questionText;

    /** 选项信息 */
    private String optionInfo;

    /** 参考答案 */
    private String refAnswer;

    /** 解析思路 */
    private String analysis;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
