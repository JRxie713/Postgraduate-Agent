package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.utils.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口（登录/测试）
 * 后续可对接真实用户表
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtils jwtUtils;

    public AuthController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /**
     * 简化版登录（仅用于脚手架演示，不校验真实密码）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return Result.error(400, "用户名不能为空");
        }

        // 生成 Token
        String token = jwtUtils.generateToken(username);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("username", username);
        return Result.success(data);
    }

    /**
     * 获取当前登录用户信息（需带 Token）
     */
    @GetMapping("/me")
    public Result<Map<String, String>> me() {
        Map<String, String> data = new HashMap<>();
        data.put("username", "current-user");
        data.put("role", "ROLE_USER");
        return Result.success(data);
    }
}
