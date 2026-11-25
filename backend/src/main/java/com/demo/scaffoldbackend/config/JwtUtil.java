package com.demo.scaffoldbackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * JWT 令牌工具：签发/解析访问令牌与刷新令牌
 */
@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-expire:7200}")
    private long accessExpire;

    @Value("${security.jwt.refresh-expire:604800}")
    private long refreshExpire;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发访问令牌 */
    public String issueAccess(Integer userId, String role) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpire * 1000))
                .signWith(key())
                .compact();
    }

    /** 签发刷新令牌（jti 保证每次刷新令牌唯一，避免同秒重复签发导致轮换失效） */
    public String issueRefresh(Integer userId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpire * 1000))
                .signWith(key())
                .compact();
    }

    /** 解析令牌，非法或过期抛异常 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 计算令牌的 SHA-256 摘要（64 位小写十六进制）
     * Redis 只保存摘要：即使缓存被读取，也拿不到可直接使用的令牌
     */
    public static String digest(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // JDK 必定内置 SHA-256，正常不会走到这里
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    public long getAccessExpire() {
        return accessExpire;
    }

    public long getRefreshExpire() {
        return refreshExpire;
    }
}
