package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 选项
 */
@Data
public class QuestionOptionVO {

    /** 选项标识，如 A */
    private String key;

    /** 选项内容 */
    private String text;
}
