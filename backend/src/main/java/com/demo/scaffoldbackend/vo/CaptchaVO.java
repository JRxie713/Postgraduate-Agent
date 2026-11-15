package com.demo.scaffoldbackend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码标识，登录时回传 */
    private String captchaId;

    /** 验证码图片（data:image/png;base64,...） */
    private String image;
}
