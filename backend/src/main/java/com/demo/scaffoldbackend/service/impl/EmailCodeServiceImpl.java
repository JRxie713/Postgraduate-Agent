package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.EmailCodeDTO;
import com.demo.scaffoldbackend.service.CaptchaService;
import com.demo.scaffoldbackend.service.EmailCodeService;
import com.demo.scaffoldbackend.vo.EmailCodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 邮箱验证码：配置 QQ 邮箱 SMTP 授权码后真实发信，未配置时只打印日志便于本地联调
 */
@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeServiceImpl.class);
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final StringRedisTemplate redis;
    private final CaptchaService captchaService;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailAuthCode;

    @Value("${security.email.expire-seconds:300}")
    private long expireSeconds;

    @Value("${security.email.cooldown-seconds:60}")
    private long cooldownSeconds;

    public EmailCodeServiceImpl(StringRedisTemplate redis, CaptchaService captchaService, JavaMailSender mailSender) {
        this.redis = redis;
        this.captchaService = captchaService;
        this.mailSender = mailSender;
    }

    @Override
    public EmailCodeVO sendLoginCode(EmailCodeDTO dto) {
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase();
        if (!email.matches(EMAIL_REGEX)) {
            throw new BusinessException(400, "请输入正确的邮箱地址");
        }
        // 发送前先校验图形验证码，防止邮件接口被刷
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());

        String cooldownKey = "email:cooldown:" + email;
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            throw new BusinessException(429, "发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set("email:login:" + email, code, Duration.ofSeconds(expireSeconds));
        redis.opsForValue().set("email:attempt:" + email, "0", Duration.ofSeconds(expireSeconds));
        redis.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));

        boolean delivered = send(email, code);
        return new EmailCodeVO(expireSeconds, cooldownSeconds, delivered);
    }

    @Override
    public void verifyLoginCode(String email, String code) {
        String key = "email:login:" + email;
        String attemptKey = "email:attempt:" + email;
        String expected = redis.opsForValue().get(key);
        if (expected == null) {
            throw new BusinessException(400, "邮箱验证码已过期，请重新获取");
        }

        Long attempts = redis.opsForValue().increment(attemptKey);
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            redis.delete(key);
            redis.delete(attemptKey);
            throw new BusinessException(400, "验证码错误次数过多，请重新获取");
        }

        if (!expected.equals(code.trim())) {
            throw new BusinessException(400, "邮箱验证码错误");
        }
        redis.delete(key);
        redis.delete(attemptKey);
    }

    private boolean send(String email, String code) {
        if (isBlank(mailUsername) || isBlank(mailAuthCode)) {
            log.warn("[MAIL-MOCK] 未配置 QQ 邮箱授权码，验证码仅打印日志：{} -> {}", email, code);
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(email);
        message.setSubject("考研 AI 导学系统 - 登录验证码");
        message.setText("您的登录验证码是：" + code + "，" + (expireSeconds / 60)
                + " 分钟内有效。如非本人操作，请忽略本邮件。");
        mailSender.send(message);
        log.info("[MAIL] 已向 {} 发送登录验证码", email);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
