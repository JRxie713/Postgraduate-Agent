package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 账号可用性校验响应体
 */
@Data
public class UserCheckVO {

    /** 用户名 */
    private String userName;

    /** 是否可用 */
    private Boolean available;

    /** 不可用原因 */
    private String reason;
}
