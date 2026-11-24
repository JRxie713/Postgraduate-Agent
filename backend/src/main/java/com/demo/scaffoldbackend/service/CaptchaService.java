package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.vo.CaptchaVO;

public interface CaptchaService {

    /** 生成图形验证码并写入 Redis */
    CaptchaVO generate();

    /** 校验验证码（一次性，校验后立即失效） */
    void verify(String captchaId, String captchaCode);

    /** 是否开启验证码 */
    boolean enabled();
}
