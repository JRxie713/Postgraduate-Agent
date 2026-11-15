package com.demo.scaffoldbackend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱验证码发送结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCodeVO {

    /** 验证码有效期（秒） */
    private long expiresIn;

    /** 重新发送冷却时间（秒） */
    private long cooldown;

    /** 是否已通过 SMTP 真正投递；false 表示未配置授权码，验证码只打印在后端日志 */
    private boolean delivered;
}
