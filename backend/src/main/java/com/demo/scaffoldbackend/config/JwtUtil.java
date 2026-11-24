package com.demo.scaffoldbackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    /** 签发本地文件临时下载票据（subject 为存储 key，type=file） */
    public String issueFileTicket(String storageKey, long expireSeconds) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(storageKey)
                .claim("type", "file")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .signWith(key())
                .compact();
    }

    /** 校验本地文件下载票据，返回存储 key；非法或过期抛异常 */
    public String parseFileTicket(String token) {
        Claims claims = parse(token);
        if (!"file".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("非文件下载令牌");
        }
        return claims.getSubject();
    }

    public long getAccessExpire() {
        return accessExpire;
    }

    public long getRefreshExpire() {
        return refreshExpire;
    }
}
