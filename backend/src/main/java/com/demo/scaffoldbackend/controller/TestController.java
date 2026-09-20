package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")

public class TestController {

    // Git协作测试 - 这行代码用于验证团队推送和拉取流程
    @GetMapping("/hello")
    public Result<Map<String, String>> hello() {
        Map<String, String> map = new HashMap<>();
        map.put("message", "考研AI导学系统后端启动成功！");
        map.put("status", "running");
        return Result.success(map);
    }

    /**
     * GET接口：接收url参数，测试带参数请求
     */
    @GetMapping("/say")
    public Result<String> say(@RequestParam String name){
        return Result.success("你好：" + name + "，系统正常");
    }

}