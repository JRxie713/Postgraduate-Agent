package com.demo.scaffoldbackend.config;

import com.demo.scaffoldbackend.service.RbacService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器：解析 Authorization 头，校验通过后写入 SecurityContext
 * 同时比对 Redis 会话，保证退出登录 / 改密 / 异地登录后旧令牌立即失效
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final RbacService rbacService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redis, RbacService rbacService) {
        this.jwtUtil = jwtUtil;
        this.redis = redis;
        this.rbacService = rbacService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                String subject = claims.getSubject();
                String role = claims.get("role", String.class);
                // 仅访问令牌可访问接口；且必须与 Redis 会话中的当前令牌一致
                if (subject != null && "access".equals(claims.get("type", String.class))
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String sessionToken = redis.opsForValue().get("auth:session:" + subject);
                    if (token.equals(sessionToken)) {
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        if (role != null && !role.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                        // 禅道式权限点：把用户所属权限组的 module:method 一并放进 authorities
                        Set<String> permissions = rbacService.permissionsOf(Integer.valueOf(subject), role);
                        for (String permission : permissions) {
                            authorities.add(new SimpleGrantedAuthority(permission));
                        }
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(subject, null, authorities);
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                // 令牌非法或过期：清空上下文，后续由 entry point 返回 401
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
