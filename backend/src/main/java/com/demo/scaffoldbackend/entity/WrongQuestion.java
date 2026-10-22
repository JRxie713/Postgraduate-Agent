package com.demo.scaffoldbackend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表7 错题本表
 */
@Data
public class WrongQuestion {

    /** 主键 */
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 题目ID */
    private Integer questionId;

    /** 错误原因 */
    private String wrongReason;

    /** 掌握程度 */
    private String masteryLevel;

    /** 加入错题时间 */
    private LocalDateTime addTime;

    /** 复习次数 */
    private Integer reviewCount;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
