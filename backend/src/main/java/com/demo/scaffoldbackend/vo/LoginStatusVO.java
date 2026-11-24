package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 登录风控状态：锁定信息与剩余尝试次数
 */
@Data
public class LoginStatusVO {

    /** 登录账号 */
    private String userName;

    /** 是否处于锁定期 */
    private boolean locked;

    /** 锁定剩余秒数（未锁定为 0） */
    private long remainSeconds;

    /** 剩余可尝试次数 */
    private int remainingAttempts;

    /** 触发锁定的失败次数阈值 */
    private int maxFail;
}
