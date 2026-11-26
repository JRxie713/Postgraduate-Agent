package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.StudyCheckinDTO;
import com.demo.scaffoldbackend.kafka.StudyCheckinEvent;
import com.demo.scaffoldbackend.kafka.StudyCheckinKafkaProducer;
import com.demo.scaffoldbackend.service.StudyCheckinService;
import com.demo.scaffoldbackend.vo.StudyCheckinVO;
import com.demo.scaffoldbackend.vo.StudyOverviewVO;
import com.demo.scaffoldbackend.vo.StudyRankVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 学习打卡服务实现
 *
 * <p>Redis 承担实时读写：分布式锁、当日去重、计数器、Hash 缓存、ZSet 排行榜；
 * Kafka 承担异步聚合：历史累计时长、缓存刷新与后续落库 / 通知扩展。</p>
 */
@Service
public class StudyCheckinServiceImpl implements StudyCheckinService {

    private static final Logger log = LoggerFactory.getLogger(StudyCheckinServiceImpl.class);

    /** 概览缓存有效期：30 分钟 */
    private static final Duration OVERVIEW_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final StudyCheckinKafkaProducer producer;

    public StudyCheckinServiceImpl(StringRedisTemplate redis, StudyCheckinKafkaProducer producer) {
        this.redis = redis;
        this.producer = producer;
    }

    // ============================== 打卡 ==============================

    @Override
    public StudyCheckinVO checkIn(StudyCheckinDTO dto) {
        Integer userId = dto.getUserId();
        int minutes = dto.getMinutes();
        String today = LocalDate.now().toString();

        // 场景一：String 分布式锁（SET NX EX）—— 防止同一用户并发提交造成重复打卡 / 重复计数
        String lockKey = "study:lock:checkin:" + userId + ":" + today;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(429, "操作太频繁，请勿重复提交打卡");
        }

        try {
            String doneKey = "study:checkin:done:" + today;
            String minutesKey = "study:minutes:" + userId + ":" + today;
            String streakKey = "study:streak:" + userId;
            String lastDateKey = "study:last-date:" + userId;

            // 场景二：Set 去重 —— 判断今天是否已经打过卡
            boolean firstCheckin = !Boolean.TRUE.equals(redis.opsForSet().isMember(doneKey, String.valueOf(userId)));

            long minutesToday = incrementMinutes(minutesKey, minutes);
            long streak;

            if (firstCheckin) {
                // 登记今日已打卡
                redis.opsForSet().add(doneKey, String.valueOf(userId));

                // 连续天数：依据上次打卡日期判断连续 / 中断 / 首日
                String lastRaw = redis.opsForValue().get(lastDateKey);
                LocalDate last = lastRaw == null ? null : LocalDate.parse(lastRaw);
                long prevStreak = readLong(streakKey, 0);
                streak = calcStreak(last, LocalDate.now(), prevStreak);

                redis.opsForValue().set(lastDateKey, today);
                redis.opsForValue().set(streakKey, String.valueOf(streak));

                // 场景五：ZSet 排行榜 —— 连续打卡榜，score 为连续天数
                redis.opsForZSet().add("study:rank:streak", String.valueOf(userId), streak);
            } else {
                // 当天补充学习时长：不重复计算连续天数
                streak = readLong(streakKey, 0);
            }

            // 场景五：ZSet 排行榜 —— 当日学习时长榜，score 为今日累计分钟
            redis.opsForZSet().add("study:rank:minutes:" + today, String.valueOf(userId), minutesToday);

            // 打卡数据已变化，先删除个人概览缓存；Kafka 消费端也会再次失效，保证不读脏数据
            redis.delete("study:overview:" + userId);

            // 场景六：Kafka 异步事件 —— 投递打卡事件，由消费端累计历史时长、刷新缓存
            StudyCheckinEvent event = new StudyCheckinEvent(
                    UUID.randomUUID().toString(),
                    userId,
                    minutes,
                    dto.getSubject(),
                    today,
                    (int) streak,
                    LocalDateTime.now().toString()
            );
            producer.sendCheckinEvent(event);

            StudyCheckinVO vo = new StudyCheckinVO();
            vo.setUserId(userId);
            vo.setDate(today);
            vo.setMinutesToday(minutesToday);
            vo.setStreak(streak);
            vo.setFirstCheckin(firstCheckin);
            return vo;
        } finally {
            // 释放锁；即便业务异常也能兜底释放，5 秒过期是防死锁的最后一道保障
            redis.delete(lockKey);
        }
    }

    // ============================== 概览（缓存读穿透） ==============================

    @Override
    public StudyOverviewVO overview(Integer userId) {
        String hashKey = "study:overview:" + userId;

        // 场景四：Hash 缓存 —— 先读缓存，命中直接返回（Cache-Aside）
        Map<Object, Object> cached = redis.opsForHash().entries(hashKey);
        if (!cached.isEmpty()) {
            StudyOverviewVO vo = new StudyOverviewVO();
            vo.setUserId(userId);
            vo.setStreak(readLong(cached.get("streak"), 0));
            vo.setLastDate((String) cached.get("lastDate"));
            vo.setMinutesToday(readLong(cached.get("minutesToday"), 0));
            vo.setTotalMinutes(readLong(cached.get("totalMinutes"), 0));
            Object rankRaw = cached.get("rank");
            vo.setRank(rankRaw == null || String.valueOf(rankRaw).isBlank()
                    ? null : readLong(rankRaw, 0));
            vo.setCacheTime((String) cached.get("cacheTime"));
            return vo;
        }

        // 缓存未命中：从各 Redis 结构汇总后回填缓存
        String today = LocalDate.now().toString();
        long streak = readLong("study:streak:" + userId, 0);
        String lastDate = redis.opsForValue().get("study:last-date:" + userId);
        long minutesToday = readLong("study:minutes:" + userId + ":" + today, 0);
        long totalMinutes = readLong("study:total-minutes:" + userId, 0);

        Long reverseRank = redis.opsForZSet().reverseRank("study:rank:streak", String.valueOf(userId));
        Long rank = reverseRank == null ? null : reverseRank + 1;

        StudyOverviewVO vo = new StudyOverviewVO();
        vo.setUserId(userId);
        vo.setStreak(streak);
        vo.setLastDate(lastDate);
        vo.setMinutesToday(minutesToday);
        vo.setTotalMinutes(totalMinutes);
        vo.setRank(rank);
        vo.setCacheTime(LocalDateTime.now().toString());

        // 回填 Hash 并设置 TTL；缓存过期后下次查询重新读穿透
        Map<String, String> entry = new HashMap<>();
        entry.put("streak", String.valueOf(streak));
        entry.put("lastDate", lastDate == null ? "" : lastDate);
        entry.put("minutesToday", String.valueOf(minutesToday));
        entry.put("totalMinutes", String.valueOf(totalMinutes));
        entry.put("rank", rank == null ? "" : String.valueOf(rank));
        entry.put("cacheTime", vo.getCacheTime());
        redis.opsForHash().putAll(hashKey, entry);
        redis.expire(hashKey, OVERVIEW_TTL);
        return vo;
    }

    // ============================== 排行榜 ==============================

    @Override
    public List<StudyRankVO> streakRank(int top) {
        return buildRank("study:rank:streak", top);
    }

    @Override
    public List<StudyRankVO> todayMinutesRank(int top) {
        return buildRank("study:rank:minutes:" + LocalDate.now(), top);
    }

    @Override
    public void evictOverviewCache(Integer userId) {
        redis.delete("study:overview:" + userId);
        log.info("已失效用户 {} 的学习概览缓存", userId);
    }

    // ============================== 私有辅助方法 ==============================

    /** 累加当日学习分钟数，并把过期时间设到次日零点之后，避免脏计数长期残留 */
    private long incrementMinutes(String minutesKey, int minutes) {
        Long value = redis.opsForValue().increment(minutesKey, minutes);
        redis.expire(minutesKey, Duration.ofDays(2));
        return value == null ? minutes : value;
    }

    /** 按上次打卡日期推算连续天数：连续 +1，中断重置为 1，首日为 1 */
    private long calcStreak(LocalDate last, LocalDate today, long prevStreak) {
        if (last == null) {
            return 1;
        }
        if (last.isEqual(today)) {
            return prevStreak;
        }
        if (last.isEqual(today.minusDays(1))) {
            return prevStreak + 1;
        }
        return 1;
    }

    /** 组装 ZSet 倒序榜单 */
    private List<StudyRankVO> buildRank(String key, int top) {
        if (top < 1 || top > 100) {
            throw new IllegalArgumentException("榜单条数必须在 1 到 100 之间");
        }
        List<StudyRankVO> result = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(key, 0, top - 1);
        if (tuples == null) {
            return result;
        }
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            StudyRankVO vo = new StudyRankVO();
            vo.setRank(rank++);
            vo.setUserId(tuple.getValue());
            vo.setScore(tuple.getScore() == null ? 0 : tuple.getScore());
            result.add(vo);
        }
        return result;
    }

    private long readLong(String key, long defaultValue) {
        // 注意：get() 返回的是 String，这里必须显式转成 Object，否则编译期会按重载规则
        // 再次绑定到本方法（把返回值当成 Redis key），而下面的 Object 重载是把入参当缓存值解析；
        // 缓存缺失时 get 返回 null，若被当作 key 传给 Redis 就会抛 "non null key required"。
        return readLong((Object) redis.opsForValue().get(key), defaultValue);
    }

    private long readLong(Object raw, long defaultValue) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
