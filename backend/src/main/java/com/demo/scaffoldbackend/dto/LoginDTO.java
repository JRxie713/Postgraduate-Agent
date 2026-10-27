package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 登录请求体
 */
@Data
public class LoginDTO {

    /** 登录账号 */
    private String userName;

    /** 密码 */
    private String pwd;
}
