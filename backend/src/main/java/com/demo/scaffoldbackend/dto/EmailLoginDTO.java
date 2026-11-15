package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮箱 + 邮箱验证码登录请求体
 */
@Data
public class EmailLoginDTO {

    /** 注册时绑定的邮箱 */
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /** 邮箱验证码 */
    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;
}
