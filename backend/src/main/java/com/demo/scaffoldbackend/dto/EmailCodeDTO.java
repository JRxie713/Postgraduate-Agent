package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送邮箱验证码请求体
 */
@Data
public class EmailCodeDTO {

    /** 接收验证码的邮箱 */
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /** 图形验证码标识 */
    private String captchaId;

    /** 图形验证码 */
    private String captchaCode;
}
