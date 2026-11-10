package com.demo.scaffoldbackend.config;

import com.demo.scaffoldbackend.common.result.Result;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * 全局限流 + IP 黑名单 + UA 校验（反爬虫）
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long globalPerMinute;
    private final long blockThreshold;
    private final long blockMinutes;
    private final boolean uaCheckEnabled;

    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper,
                           boolean enabled, long globalPerMinute, long blockThreshold,
                           long blockMinutes, boolean uaCheckEnabled) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.globalPerMinute = globalPerMinute;
        this.blockThreshold = blockThreshold;
        this.blockMinutes = blockMinutes;
        this.uaCheckEnabled = uaCheckEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled || shouldSkip(request)) {
            chain.doFilter(request, response);
            return;
        }
        String ip = clientIp(request);

        // IP 黑名单
        if (Boolean.TRUE.equals(redis.hasKey("blacklist:ip:" + ip))) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "访问过于频繁，已被临时封禁");
            return;
        }

        // UA 校验（默认关闭）
        if (uaCheckEnabled && isBlank(request.getHeader("User-Agent"))) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, 400, "非法请求");
            return;
        }

        // 固定窗口限流（按分钟）
        long window = System.currentTimeMillis() / 60000L;
        String key = "ratelimit:" + ip + ":" + window;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > blockThreshold) {
            redis.opsForValue().set("blacklist:ip:" + ip, "blocked", Duration.ofMinutes(blockMinutes));
            writeJson(response, 429, 429, "访问过于频繁，已被临时封禁");
            return;
        }
        if (count != null && count > globalPerMinute) {
            writeJson(response, 429, 429, "请求过于频繁，请稍后再试");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeJson(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
