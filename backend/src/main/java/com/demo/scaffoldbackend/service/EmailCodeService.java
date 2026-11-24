package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.EmailCodeDTO;
import com.demo.scaffoldbackend.vo.EmailCodeVO;

public interface EmailCodeService {

    /** 校验图形验证码后发送邮箱验证码 */
    EmailCodeVO sendLoginCode(EmailCodeDTO dto);

    /** 校验邮箱验证码（一次性，成功后立即失效） */
    void verifyLoginCode(String email, String code);
}
