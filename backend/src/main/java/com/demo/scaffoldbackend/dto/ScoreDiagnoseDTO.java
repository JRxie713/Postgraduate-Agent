package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 模考后能力诊断请求体
 */
@Data
public class ScoreDiagnoseDTO {

    /** 模考ID */
    private Integer mockExamId;

    /** 用户ID */
    private Integer userId;

    /** 是否回写备考画像 */
    private Boolean updateProfile;
}
