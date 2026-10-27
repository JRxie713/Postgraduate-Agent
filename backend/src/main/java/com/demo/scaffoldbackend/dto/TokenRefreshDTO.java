package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 刷新令牌请求体
 */
@Data
public class TokenRefreshDTO {

    /** 刷新令牌 */
    private String refreshToken;
}
