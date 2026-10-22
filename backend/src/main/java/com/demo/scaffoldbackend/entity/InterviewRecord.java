package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表10 复试训练记录表
 */
@Data
public class InterviewRecord {

    /** 主键 */
    private Integer id;

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

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
