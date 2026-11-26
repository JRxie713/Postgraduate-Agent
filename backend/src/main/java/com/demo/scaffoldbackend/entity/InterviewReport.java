package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试最终报告表（五维评分 + 综合评语）
 */
@Data
public class InterviewReport {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 所属会话ID */
    private Integer sessionId;

    /** 表达能力(0-100) */
    private Double expressionScore;

    /** 专业知识(0-100) */
    private Double professionalScore;

    /** 逻辑思维(0-100) */
    private Double logicScore;

    /** 英语能力(0-100,未练英语时与表达分一致) */
    private Double englishScore;

    /** 应变能力(0-100) */
    private Double adaptabilityScore;

    /** 加权总分(0-100) */
    private Double totalScore;

    /** AI 综合总评(一段话总结) */
    private String summary;

    /** 主要问题(JSON数组或换行文本) */
    private String mainIssues;

    /** 改进建议(JSON数组或换行文本) */
    private String suggestions;

    /** 用户是否已阅读(1=已读,0=未读,定时任务扫描用) */
    private Integer reportRead;

    /** 本次报告是否为规则化兜底(1=是) */
    private Integer ruleBased;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
