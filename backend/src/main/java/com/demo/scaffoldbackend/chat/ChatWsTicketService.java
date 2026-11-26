package com.demo.scaffoldbackend.chat;

import com.demo.scaffoldbackend.config.ChatProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * WebSocket 一次性连接票据：避免把长期 JWT 暴露在 WebSocket URL 上
 *
 * <p>票据存 Redis，60 秒内有效且只能消费一次；多实例部署时各实例共享同一份票据。</p>
 */
@Service
public class ChatWsTicketService {

    private static final String TICKET_KEY_PREFIX = "chat:ticket:";

    // GETDEL 需要 Redis 6.2+；用 Lua 原子地 GET + DEL，兼容低版本且仍保证票据一次性
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]) if v then redis.call('DEL', KEYS[1]) end return v",
            String.class);

    private final StringRedisTemplate redis;
    private final ChatProperties properties;

    public ChatWsTicketService(StringRedisTemplate redis, ChatProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** 签发一次性票据 */
    public String issue(Integer userId) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(TICKET_KEY_PREFIX + ticket, String.valueOf(userId),
                Duration.ofSeconds(properties.getTicketTtlSeconds()));
        return ticket;
    }

    /** 消费票据，返回用户ID；票据不存在/已使用时返回 null */
    public Integer consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        String value = redis.execute(CONSUME_SCRIPT, Collections.singletonList(TICKET_KEY_PREFIX + ticket));
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
