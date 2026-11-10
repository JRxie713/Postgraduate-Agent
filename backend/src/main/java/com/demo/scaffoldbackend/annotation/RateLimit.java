package com.demo.scaffoldbackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级限流注解，配合 RateLimitInterceptor 使用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的最大次数 */
    int limit() default 10;

    /** 窗口时长（秒） */
    int window() default 60;

    /** 限流维度：ip / user */
    String key() default "ip";
}
