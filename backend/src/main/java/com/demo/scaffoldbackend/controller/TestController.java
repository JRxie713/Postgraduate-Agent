package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.springframework.web.bind.annotation.*;

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
     * POST接口：JSON提交测试
     */
    @PostMapping("/submit")
    public Result<Map<String,Object>> submit(@RequestBody Map<String,Object> data){
        Map<String,Object> res = new HashMap<>();
        res.put("receiveData", data);
        res.put("msg","POST数据接收成功");
        return Result.success(res);
    }

}