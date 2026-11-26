package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话表（一次模拟面试 = 一个 session）
 */
@Data
public class InterviewSession {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 训练目标专业 */
    private String major;

    /** 训练类型: comprehensive/professional/pressure/english */
    private String trainingType;

    /** 难度: easy/medium/hard */
    private String difficulty;

    /** 是否开启压力面试(1=是,0=否) */
    private Integer pressureMode;

    /** 计划轮次(由题库抽题数量决定) */
    private Integer totalRounds;

    /** 已完成轮次 */
    private Integer currentRound;

    /** 会话状态: ongoing/finished/abandoned */
    private String status;

    /** 最终总分(0-100),结束时由报告回填 */
    private Double score;

    /** 开始时间 */
    private LocalDateTime createTime;

    /** 结束时间 */
    private LocalDateTime finishTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
