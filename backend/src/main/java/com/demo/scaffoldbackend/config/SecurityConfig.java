package com.demo.scaffoldbackend.config;

import com.demo.scaffoldbackend.common.result.Result;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置：RBAC 认证授权
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUtil jwtUtil,
                                                   StringRedisTemplate redis,
                                                   com.demo.scaffoldbackend.service.RbacService rbacService,
                                                   @Value("${security.rate-limit.enabled:true}") boolean rateEnabled,
                                                   @Value("${security.rate-limit.global-per-minute:60}") long globalPerMinute,
                                                   @Value("${security.rate-limit.block-threshold:300}") long blockThreshold,
                                                   @Value("${security.rate-limit.block-minutes:10}") long blockMinutes,
                                                   @Value("${security.rate-limit.ua-check-enabled:false}") boolean uaCheckEnabled) throws Exception {
        // 直接实例化，避免 Filter 以 Bean 形式被 Boot 再注册一次导致重复执行
        RateLimitFilter rateLimitFilter = new RateLimitFilter(redis, objectMapper, rateEnabled,
                globalPerMinute, blockThreshold, blockMinutes, uaCheckEnabled);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, redis, rbacService);

        http.csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/login", "/api/user/register",
                        "/api/user/check", "/api/user/refresh",
                        "/api/user/captcha", "/api/user/login/status",
                        "/api/user/sms-code", "/api/user/login/sms",
                        "/api/user/email-code", "/api/user/login/email").permitAll()
                // 学习打卡演示模块（Redis + Kafka）：本地联调放开，生产环境可移除该行并改为登录鉴权
                .requestMatchers("/api/study-checkin/**", "/checkin.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录或令牌已失效"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问该资源")))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private void writeJson(HttpServletResponse response, int status, int code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
