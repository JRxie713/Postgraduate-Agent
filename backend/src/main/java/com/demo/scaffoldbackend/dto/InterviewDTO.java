package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 复试训练请求体（开启面试 / 提交作答共用）
 */
@Data
public class InterviewDTO {

    /** 用户ID */
    private Integer userId;

    /** 专业 */
    private String major;

    /** 题目数量 */
    private Integer questionCount;

    /** 是否包含英语环节 */
    private Boolean includeEnglish;

    /** 难度 */
    private String difficulty;

    /** 复试记录ID */
    private Integer recordId;

    /** 作答音频地址 */
    private String answerAudio;

    /** 作答文本 */
    private String answerText;
}
