package com.demo.scaffoldbackend.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.scaffoldbackend.entity.PolicyNotice;
import com.demo.scaffoldbackend.entity.SchoolMajor;
import com.demo.scaffoldbackend.mapper.PolicyNoticeMapper;
import com.demo.scaffoldbackend.mapper.SchoolMajorMapper;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 缓存预热：启动预加载热点数据进 Redis，并定时刷新
 */
@Component
public class CacheWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final PolicyNoticeMapper policyNoticeMapper;
    private final SchoolMajorMapper schoolMajorMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CacheWarmupRunner(PolicyNoticeMapper policyNoticeMapper, SchoolMajorMapper schoolMajorMapper,
                             StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.policyNoticeMapper = policyNoticeMapper;
        this.schoolMajorMapper = schoolMajorMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        warmup();
    }

    /** 每日凌晨 2 点刷新 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void warmup() {
        try {
            List<PolicyNotice> policies = policyNoticeMapper.selectList(
                    new LambdaQueryWrapper<PolicyNotice>()
                            .orderByDesc(PolicyNotice::getCreateTime).last("LIMIT 20"));
            redis.opsForValue().set("cache:policy:list",
                    objectMapper.writeValueAsString(policies), Duration.ofHours(1));

            List<SchoolMajor> hot = schoolMajorMapper.selectList(
                    new LambdaQueryWrapper<SchoolMajor>()
                            .orderByDesc(SchoolMajor::getScoreLine).last("LIMIT 20"));
            redis.opsForValue().set("cache:school:hot",
                    objectMapper.writeValueAsString(hot), Duration.ofHours(6));

            log.info("缓存预热完成：policy={}, school={}", policies.size(), hot.size());
        } catch (Exception e) {
            // 数据库未就绪时不应阻断启动
            log.warn("缓存预热失败（数据库可能未就绪）：{}", e.getMessage());
        }
    }
}
