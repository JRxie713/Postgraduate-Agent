package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 打卡结果响应体
 */
@Data
public class StudyCheckinVO {

    /** 用户 ID */
    private Integer userId;

    /** 打卡日期，yyyy-MM-dd */
    private String date;

    /** 今日累计学习分钟数 */
    private Long minutesToday;

    /** 当前连续打卡天数 */
    private Long streak;

    /** 本次是否为当日首次打卡（false 表示当天补充学习时长） */
    private Boolean firstCheckin;
}
