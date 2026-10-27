package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目内容响应体
 */
@Data
public class QuestionContentVO {

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
