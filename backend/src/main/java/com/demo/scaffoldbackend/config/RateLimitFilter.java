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
    private final long filePerMinute;
    private final long blockThreshold;
    private final long fileBlockThreshold;
    private final long blockMinutes;
    private final boolean uaCheckEnabled;

    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper,
                           boolean enabled, long globalPerMinute, long filePerMinute,
                           long blockThreshold, long fileBlockThreshold,
                           long blockMinutes, boolean uaCheckEnabled) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.globalPerMinute = globalPerMinute;
        this.filePerMinute = filePerMinute;
        this.blockThreshold = blockThreshold;
        this.fileBlockThreshold = fileBlockThreshold;
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

        // 文件接口（分片上传/下载）天然高频，给一份单独的、更宽但仍有上限的配额，
        // 不再像以前那样整体豁免，避免上传接口被打成免限流的写盘入口
        boolean fileApi = request.getRequestURI().startsWith("/api/file");
        long limit = fileApi ? filePerMinute : globalPerMinute;
        long blockAt = fileApi ? fileBlockThreshold : blockThreshold;

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

        // 固定窗口限流（按分钟），普通接口和文件接口各用一个计数桶
        long window = System.currentTimeMillis() / 60000L;
        String key = "ratelimit:" + (fileApi ? "file:" : "api:") + ip + ":" + window;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > blockAt) {
            redis.opsForValue().set("blacklist:ip:" + ip, "blocked", Duration.ofMinutes(blockMinutes));
            writeJson(response, 429, 429, "访问过于频繁，已被临时封禁");
            return;
        }
        if (count != null && count > limit) {
            writeJson(response, 429, 429, "请求过于频繁，请稍后再试");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 只有健康检查完全跳过；文件接口不再豁免，改为在 doFilterInternal 里用独立配额
        return uri.startsWith("/actuator");
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
