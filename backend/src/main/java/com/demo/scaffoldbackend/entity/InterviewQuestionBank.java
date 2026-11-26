package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试题库表（AI 不负责出题，面试题从这里抽）
 */
@Data
public class InterviewQuestionBank {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 目标专业 */
    private String major;

    /** 训练类型: comprehensive/professional/pressure/english */
    private String category;

    /** 难度: easy/medium/hard */
    private String difficulty;

    /** 面试题正文 */
    private String question;

    /** 考察要点(逗号分隔,用于关键词评分兜底) */
    private String keyPoints;

    /** 参考答案要点(JSON或文本,供规则化打分用) */
    private String reference;

    /** 是否压力面试题(1=是,0=否) */
    private Integer isPressure;

    /** 是否追问模板(1=是,0=否) */
    private Integer isFollowup;

    /** 是否启用(1=启用,0=禁用) */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
