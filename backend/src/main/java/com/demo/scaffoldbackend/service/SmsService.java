package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.SmsCodeDTO;
import com.demo.scaffoldbackend.vo.SmsCodeVO;

public interface SmsService {

    /** 校验图形验证码后发送短信验证码 */
    SmsCodeVO sendLoginCode(SmsCodeDTO dto);

    /** 校验短信验证码（一次性，成功后立即失效） */
    void verifyLoginCode(String phone, String code);
}
