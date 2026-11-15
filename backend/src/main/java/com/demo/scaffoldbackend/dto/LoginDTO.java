package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体
 */
@Data
public class LoginDTO {

    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    private String userName;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String pwd;

    /** 验证码标识（获取验证码接口返回） */
    private String captchaId;

    /** 图形验证码 */
    private String captchaCode;
}
