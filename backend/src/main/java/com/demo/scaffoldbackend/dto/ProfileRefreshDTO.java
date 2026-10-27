package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 备考画像重算请求体
 */
@Data
public class ProfileRefreshDTO {

    /** 用户ID */
    private Integer userId;

    /** 触发来源 */
    private String trigger;

    /** 是否强制重算 */
    private Boolean force;
}
