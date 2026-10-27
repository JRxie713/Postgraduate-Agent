package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 备考画像新增/更新请求体
 */
@Data
public class UserProfileDTO {

    /** 用户ID */
    private Integer userId;

    /** 本科专业 */
    private String undergradMajor;

    /** 目标院校 */
    private String targetSchool;

    /** 目标专业 */
    private String targetMajor;

    /** 目标地区 */
    private String targetRegion;

    /** 考研年份 */
    private Integer examYear;

    /** 基础评测分 */
    private Double baseScore;

    /** 预估分数 */
    private Double predictScore;

    /** 择校偏好 */
    private String difficultyPref;

    /** 薄弱点 */
    private String weakPoints;

    /** 备考阶段 */
    private String examStage;

    /** 情绪状态 */
    private String emotionState;
}
