package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.SmsCodeDTO;
import com.demo.scaffoldbackend.service.CaptchaService;
import com.demo.scaffoldbackend.service.SmsService;
import com.demo.scaffoldbackend.vo.SmsCodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 短信验证码：默认本地模拟发送（控制台输出 + 开发模式回显）
 * 接入阿里云/腾讯云短信时，只需替换 send() 内部实现
 */
@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final CaptchaService captchaService;
    private final SecureRandom random = new SecureRandom();

    @Value("${security.sms.expire-seconds:300}")
    private long expireSeconds;

    @Value("${security.sms.cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${security.sms.expose-code:false}")
    private boolean exposeCode;

    public SmsServiceImpl(StringRedisTemplate redis, CaptchaService captchaService) {
        this.redis = redis;
        this.captchaService = captchaService;
    }

    @Override
    public SmsCodeVO sendLoginCode(SmsCodeDTO dto) {
        String phone = dto.getPhone().trim();
        // 发送前先校验图形验证码，防止短信接口被刷
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());

        String cooldownKey = "sms:cooldown:" + phone;
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            throw new BusinessException(429, "发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set("sms:login:" + phone, code, Duration.ofSeconds(expireSeconds));
        redis.opsForValue().set("sms:attempt:" + phone, "0", Duration.ofSeconds(expireSeconds));
        redis.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));

        send(phone, code);
        return new SmsCodeVO(expireSeconds, cooldownSeconds, exposeCode ? code : null);
    }

    @Override
    public void verifyLoginCode(String phone, String code) {
        String codeKey = "sms:login:" + phone;
        String attemptKey = "sms:attempt:" + phone;
        String expected = redis.opsForValue().get(codeKey);
        if (expected == null) {
            throw new BusinessException(400, "短信验证码已过期，请重新获取");
        }

        Long attempts = redis.opsForValue().increment(attemptKey);
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            redis.delete(codeKey);
            redis.delete(attemptKey);
            throw new BusinessException(400, "验证码错误次数过多，请重新获取");
        }

        if (!expected.equals(code.trim())) {
            throw new BusinessException(400, "短信验证码错误");
        }
        redis.delete(codeKey);
        redis.delete(attemptKey);
    }

    /**
     * 本地模拟短信通道：仅打印日志。
     * 生产环境替换为短信服务商 SDK 调用，并把 expose-code 设为 false。
     */
    private void send(String phone, String code) {
        log.info("[SMS-MOCK] 向 {} 发送登录验证码：{}（{} 分钟内有效）", phone, code, expireSeconds / 60);
    }
}
