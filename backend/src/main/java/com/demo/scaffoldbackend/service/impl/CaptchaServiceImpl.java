package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.service.CaptchaService;
import com.demo.scaffoldbackend.vo.CaptchaVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码：Java2D 绘制，结果存 Redis，一次性校验
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 去掉了易混淆字符 0/O、1/I */
    private static final char[] SYMBOLS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;

    @Value("${security.captcha.enabled:true}")
    private boolean enabled;

    @Value("${security.captcha.expire-seconds:120}")
    private long expireSeconds;

    public CaptchaServiceImpl(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public CaptchaVO generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(SYMBOLS[random.nextInt(SYMBOLS.length)]);
        }
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(captchaKey(captchaId), code.toString(), Duration.ofSeconds(expireSeconds));
        return new CaptchaVO(captchaId, "data:image/png;base64," + draw(code.toString()));
    }

    @Override
    public void verify(String captchaId, String captchaCode) {
        if (!enabled) {
            return;
        }
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(400, "请输入图形验证码");
        }
        String key = captchaKey(captchaId);
        String expected = redis.opsForValue().get(key);
        // 一次性使用：无论对错都删除，防止复用暴力尝试
        redis.delete(key);
        if (expected == null) {
            throw new BusinessException(400, "验证码已过期，请点击图片刷新");
        }
        if (!expected.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(400, "验证码错误");
        }
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    private String captchaKey(String captchaId) {
        return "captcha:" + captchaId;
    }

    private String draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(248, 250, 252));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(140, 200));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            // 噪点
            for (int i = 0; i < 40; i++) {
                image.setRGB(random.nextInt(WIDTH), random.nextInt(HEIGHT), randomColor(120, 200).getRGB());
            }
            // 字符
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                double angle = (random.nextDouble() - 0.5) * 0.5;
                int x = 14 + i * 26;
                int y = 29 + random.nextInt(4);
                g.rotate(angle, x, y);
                g.setColor(randomColor(20, 100));
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(500, "验证码生成失败");
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }
}
