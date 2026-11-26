package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class DashboardController {
    private static final String PREFIX = "postgraduate:";
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public DashboardController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    /**
     * 数据大屏一次刷新所需的全部数据。
     *
     * 前端使用该聚合接口，避免一次刷新拆成多个 HTTP 请求而快速触发全局限流。
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public Result<Map<String, Object>> summary() {
        boolean student = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
        if (student) {
            return Result.success(studentSummary(currentUserId()));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", "ADMIN");
        data.put("overview", overview().getData());
        data.put("trend", trend(7).getData());
        data.put("subjects", subjectHours().getData());
        data.put("tasks", taskCompletion().getData());
        data.put("schools", schoolHot(5).getData());
        data.put("activities", activities(10).getData());
        return Result.success(data);
    }

    private Map<String, Object> studentSummary(Integer userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", "STUDENT");
        data.put("todayMinutes", number("select coalesce(sum(duration), 0) from learning_log where user_id=? and date(log_date)=curdate()", userId));
        Map<String, Object> tasks = new LinkedHashMap<>();
        tasks.put("done", number("select count(*) from plan_task t join study_plan p on t.plan_id=p.id where p.user_id=? and t.task_status='DONE'", userId));
        tasks.put("todo", number("select count(*) from plan_task t join study_plan p on t.plan_id=p.id where p.user_id=? and t.task_status='TODO'", userId));
        data.put("tasks", tasks);
        data.put("schools", schoolHot(5).getData());
        return data;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", count("select count(*) from user"));
        data.put("todayActive", redis.opsForSet().size(activeKey(LocalDate.now())));
        data.put("todayMinutes", number("select coalesce(sum(duration), 0) from learning_log where date(log_date)=curdate()"));
        data.put("todayTasks", number("select count(*) from plan_task where task_status='DONE' and date(update_time)=curdate()"));
        return Result.success(data);
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> subjectHours() {
        Map<Object, Object> values = redis.opsForHash().entries(PREFIX + "metrics:subject:" + LocalDate.now());
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(String.valueOf(key), value));
        return Result.success(result);
    }

    @GetMapping("/task-completion")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> taskCompletion() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("done", number("select count(*) from plan_task where task_status='DONE'"));
        result.put("todo", number("select count(*) from plan_task where task_status='TODO'"));
        return Result.success(result);
    }

    @GetMapping("/school-hot")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
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
    @PreAuthorize("hasRole('ADMIN')")
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

    private long number(String sql, Object... args) {
        Number value = jdbc.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    private Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("登录状态已失效，请重新登录");
        }
    }
}
