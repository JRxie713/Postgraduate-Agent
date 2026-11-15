package com.demo.scaffoldbackend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短信验证码发送结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsCodeVO {

    /** 验证码有效期（秒） */
    private long expiresIn;

    /** 重新发送冷却时间（秒） */
    private long cooldown;

    /** 开发模式回显验证码；接入真实短信服务后为 null */
    private String devCode;
}
