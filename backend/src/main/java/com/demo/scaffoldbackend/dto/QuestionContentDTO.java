package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 题目内容新增/更新请求体
 */
@Data
public class QuestionContentDTO {

    /** 题干 */
    private String questionText;

    /** 选项信息 */
    private String optionInfo;

    /** 参考答案 */
    private String refAnswer;

    /** 解析思路 */
    private String analysis;
}
