package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 复试训练记录新增/更新请求体
 */
@Data
public class InterviewRecordDTO {

    /** 用户ID */
    private Integer userId;

    /** 复试专业 */
    private String major;

    /** 面试问题 */
    private String question;

    /** 音频地址 */
    private String answerAudio;

    /** 面试得分 */
    private Double score;

    /** 压力测试评价 */
    private String pressureTest;
}
