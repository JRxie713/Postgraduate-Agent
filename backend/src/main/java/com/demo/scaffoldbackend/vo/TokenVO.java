package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 令牌响应体
 */
@Data
public class TokenVO {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 有效期（秒） */
    private Long expiresIn;
}
