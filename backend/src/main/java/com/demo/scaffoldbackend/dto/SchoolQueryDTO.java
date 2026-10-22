package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 择校请求体（冲稳保推荐 / 院校对比共用）
 */
@Data
public class SchoolQueryDTO {

    /** 用户ID */
    private Integer userId;

    /** 目标专业 */
    private String targetMajor;

    /** 预估分数 */
    private Double predictScore;

    /** 难度偏好 */
    private String difficultyPref;

    /** 返回条数 */
    private Integer limit;

    /** 对比的院校专业ID */
    private List<Integer> schoolMajorIds;
}
