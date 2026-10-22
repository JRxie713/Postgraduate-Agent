package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 修改密码请求体
 */
@Data
public class PasswordChangeDTO {

    /** 用户ID */
    private Integer userId;

    /** 原密码 */
    private String oldPwd;

    /** 新密码 */
    private String newPwd;
}
