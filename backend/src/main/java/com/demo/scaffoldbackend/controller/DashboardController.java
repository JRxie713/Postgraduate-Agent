package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员数据大屏：MySQL 是主数据来源，Redis 保存短期实时指标与排行榜。
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {
    private static final String PREFIX = "postgraduate:";
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public DashboardController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", count("select count(*) from user"));
        data.put("todayActive", redis.opsForSet().size(activeKey(LocalDate.now())));
        data.put("todayMinutes", number("select coalesce(sum(duration), 0) from learning_log where date(log_date)=curdate()"));
        data.put("todayTasks", number("select count(*) from plan_task where task_status='DONE' and date(update_time)=curdate()"));
        return Result.success(data);
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 30);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = safeDays - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.add(Map.of("date", date.format(DateTimeFormatter.ofPattern("MM-dd")),
                    "active", Optional.ofNullable(redis.opsForSet().size(activeKey(date))).orElse(0L)));
        }
        return Result.success(result);
    }

    @GetMapping("/subject-hours")
    public Result<Map<String, Object>> subjectHours() {
        Map<Object, Object> values = redis.opsForHash().entries(PREFIX + "metrics:subject:" + LocalDate.now());
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(String.valueOf(key), value));
        return Result.success(result);
    }

    @GetMapping("/task-completion")
    public Result<Map<String, Object>> taskCompletion() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("done", number("select count(*) from plan_task where task_status='DONE'"));
        result.put("todo", number("select count(*) from plan_task where task_status='TODO'"));
        return Result.success(result);
    }

    @GetMapping("/school-hot")
    public Result<List<Map<String, Object>>> schoolHot(@RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Map<String, Object>> result = Optional.ofNullable(
                        redis.opsForZSet().reverseRangeWithScores(PREFIX + "school:hot", 0, safeLimit - 1))
                .orElse(Set.of()).stream()
                .map(item -> Map.<String, Object>of("name", item.getValue(), "heat", item.getScore().longValue()))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/activities")
    public Result<List<String>> activities(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(Optional.ofNullable(redis.opsForList().range(PREFIX + "activity", 0,
                Math.min(Math.max(limit, 1), 50) - 1)).orElse(List.of()));
    }

    private String activeKey(LocalDate date) {
        return PREFIX + "active:" + date;
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private long number(String sql) {
        Number value = jdbc.queryForObject(sql, Number.class);
        return value == null ? 0L : value.longValue();
    }
}
