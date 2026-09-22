package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")

public class TestController {

    // Git协作测试
    @GetMapping("/hello")
    public Result<Map<String, String>> hello() {
        Map<String, String> map = new HashMap<>();
        map.put("message", "考研AI导学系统后端启动成功！");
        map.put("status", "running");
        return Result.success(map);
    }



}