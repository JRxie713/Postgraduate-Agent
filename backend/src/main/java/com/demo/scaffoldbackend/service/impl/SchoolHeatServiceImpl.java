package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.SchoolHeatDTO;
import com.demo.scaffoldbackend.kafka.SchoolHeatEvent;
import com.demo.scaffoldbackend.kafka.SchoolHeatKafkaProducer;
import com.demo.scaffoldbackend.service.SchoolHeatService;
import com.demo.scaffoldbackend.vo.SchoolCardVO;
import com.demo.scaffoldbackend.vo.SchoolHeatVO;
import com.demo.scaffoldbackend.vo.SchoolOptionVO;
import com.demo.scaffoldbackend.vo.SchoolTrackVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 院校热度服务实现
 *
 * <p>Redis 承担实时读写：当日去重 Set、动作计数 Hash、粉丝 / 关注集合、ZSet 热度榜；
 * Kafka 承担异步聚合：历史浏览量 / 关注量 / 总热度，以及后续落库、推荐等扩展。</p>
 */
@Service
public class SchoolHeatServiceImpl implements SchoolHeatService {

    private static final Logger log = LoggerFactory.getLogger(SchoolHeatServiceImpl.class);

    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_FOLLOW = "FOLLOW";
    private static final String ACTION_UNFOLLOW = "UNFOLLOW";

    /** 关注在总热度中的权重：一次关注顶 3 次浏览 */
    private static final int FOLLOW_WEIGHT = 3;

    /** 热度 ZSet，member=院校名称，score=总热度；与数据大屏 school-hot 接口共用同一结构 */
    private static final String HOT_ZSET = "postgraduate:school:hot";

    private final StringRedisTemplate redis;
    private final SchoolHeatKafkaProducer producer;
    private final JdbcTemplate jdbcTemplate;

    public SchoolHeatServiceImpl(StringRedisTemplate redis,
                                 SchoolHeatKafkaProducer producer,
                                 JdbcTemplate jdbcTemplate) {
        this.redis = redis;
        this.producer = producer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SchoolHeatVO record(SchoolHeatDTO dto) {
        Integer userId = dto.getUserId();
        String schoolName = dto.getSchoolName() == null ? "" : dto.getSchoolName().trim();
        String action = normalizeAction(dto.getAction());

        switch (action) {
            case ACTION_VIEW -> recordView(userId, schoolName, dto.getSchoolId());
            case ACTION_FOLLOW -> recordFollow(userId, schoolName, dto.getSchoolId());
            case ACTION_UNFOLLOW -> recordUnfollow(userId, schoolName, dto.getSchoolId());
            default -> throw new BusinessException(400, "不支持的动作类型：" + action);
        }

        return toVO(schoolName, dto.getSchoolId(), score(schoolName));
    }

    @Override
    public List<SchoolHeatVO> rank(int top) {
        if (top < 1 || top > 100) {
            throw new IllegalArgumentException("榜单条数必须在 1 到 100 之间");
        }
        // 院校名称 -> 记录 ID，用于榜单回填 schoolId
        Map<String, Integer> nameToId = new HashMap<>();
        for (SchoolOptionVO option : options()) {
            nameToId.put(option.getSchoolName(), option.getSchoolId());
        }

        List<SchoolHeatVO> result = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(HOT_ZSET, 0, top - 1);
        if (tuples == null) {
            return result;
        }
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String name = tuple.getValue();
            Double heat = tuple.getScore() == null ? 0d : tuple.getScore();
            SchoolHeatVO vo = toVO(name, nameToId.get(name), heat);
            vo.setRank(rank++);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<SchoolOptionVO> options() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT MIN(id) AS id, school FROM school_major GROUP BY school ORDER BY id");
        List<SchoolOptionVO> options = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object school = row.get("school");
            if (id != null && school != null) {
                options.add(new SchoolOptionVO(((Number) id).intValue(), String.valueOf(school)));
            }
        }
        return options;
    }

    @Override
    public List<SchoolCardVO> cards(Integer userId) {
        List<SchoolCardVO> cards = new ArrayList<>();
        for (SchoolOptionVO option : options()) {
            String school = option.getSchoolName();
            long follows = followerCount(school);
            boolean followed = userId != null && Boolean.TRUE.equals(
                    redis.opsForSet().isMember(followingKey(userId), school));
            long views = readLong(viewKey(school), 0);
            Double heat = score(school);
            cards.add(new SchoolCardVO(option.getSchoolId(), school, follows, followed, views, heat));
        }
        return cards;
    }

    @Override
    public List<SchoolTrackVO> myTracks(Integer userId) {
        String seenKey = seenKey(userId);
        Set<String> members = redis.opsForSet().members(seenKey);
        List<SchoolTrackVO> tracks = new ArrayList<>();
        if (members == null) {
            return tracks;
        }
        for (String member : members) {
            int idx = member.lastIndexOf(':');
            String schoolName = idx > 0 ? member.substring(0, idx) : member;
            String action = idx > 0 ? member.substring(idx + 1) : "VIEW";
            tracks.add(new SchoolTrackVO(schoolName, action));
        }
        return tracks;
    }

    // ============================== 行为记录 ==============================

    /** 浏览：当日同校同动作去重，浏览计 1 分 */
    private void recordView(Integer userId, String schoolName, Integer schoolId) {
        String seenKey = seenKey(userId);
        String member = schoolName + ":" + ACTION_VIEW;
        boolean firstTime = !Boolean.TRUE.equals(redis.opsForSet().isMember(seenKey, member));

        if (firstTime) {
            redis.opsForSet().add(seenKey, member);
            redis.expire(seenKey, Duration.ofDays(2));

            String actionHashKey = "postgraduate:heat:action:" + schoolName;
            redis.opsForHash().increment(actionHashKey, "views", 1);
            redis.expire(actionHashKey, Duration.ofDays(2));

            redis.opsForZSet().incrementScore(HOT_ZSET, schoolName, 1);
            sendEvent(userId, schoolId, schoolName, ACTION_VIEW);
        }
    }

    /** 关注：幂等加入粉丝 / 关注集合，关注计 3 分 */
    private void recordFollow(Integer userId, String schoolName, Integer schoolId) {
        String followerKey = followersKey(schoolName);
        if (Boolean.TRUE.equals(redis.opsForSet().isMember(followerKey, String.valueOf(userId)))) {
            // 已关注：重复点击不重复加分、不重复计数
            return;
        }
        redis.opsForSet().add(followerKey, String.valueOf(userId));
        redis.opsForSet().add(followingKey(userId), schoolName);

        // 当日明细：记录一条关注行为（同日同校去重）
        redis.opsForSet().add(seenKey(userId), schoolName + ":" + ACTION_FOLLOW);
        redis.expire(seenKey(userId), Duration.ofDays(2));

        String actionHashKey = "postgraduate:heat:action:" + schoolName;
        redis.opsForHash().increment(actionHashKey, "follows", 1);
        redis.expire(actionHashKey, Duration.ofDays(2));

        redis.opsForZSet().incrementScore(HOT_ZSET, schoolName, FOLLOW_WEIGHT);
        sendEvent(userId, schoolId, schoolName, ACTION_FOLLOW);
    }

    /** 取关：移出粉丝 / 关注集合，回退 3 分 */
    private void recordUnfollow(Integer userId, String schoolName, Integer schoolId) {
        String followerKey = followersKey(schoolName);
        if (!Boolean.TRUE.equals(redis.opsForSet().isMember(followerKey, String.valueOf(userId)))) {
            // 未关注：重复取关直接忽略
            return;
        }
        redis.opsForSet().remove(followerKey, String.valueOf(userId));
        redis.opsForSet().remove(followingKey(userId), schoolName);

        // 当日明细：移除该院校的关注记录
        redis.opsForSet().remove(seenKey(userId), schoolName + ":" + ACTION_FOLLOW);

        String actionHashKey = "postgraduate:heat:action:" + schoolName;
        redis.opsForHash().increment(actionHashKey, "follows", -1);
        redis.expire(actionHashKey, Duration.ofDays(2));

        redis.opsForZSet().incrementScore(HOT_ZSET, schoolName, -FOLLOW_WEIGHT);
        sendEvent(userId, schoolId, schoolName, ACTION_UNFOLLOW);
    }

    private void sendEvent(Integer userId, Integer schoolId, String schoolName, String action) {
        SchoolHeatEvent event = new SchoolHeatEvent(
                UUID.randomUUID().toString(),
                userId,
                schoolId,
                schoolName,
                action,
                LocalDate.now().toString()
        );
        producer.sendHeatEvent(event);
    }

    // ============================== 私有辅助方法 ==============================

    /** 归一化动作类型；非法值抛业务异常 */
    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return ACTION_VIEW;
        }
        String upper = action.trim().toUpperCase();
        if (ACTION_VIEW.equals(upper) || ACTION_FOLLOW.equals(upper) || ACTION_UNFOLLOW.equals(upper)) {
            return upper;
        }
        throw new BusinessException(400, "动作类型必须是 VIEW（浏览）、FOLLOW（关注）或 UNFOLLOW（取关）");
    }

    /** 读取某院校当前总热度（ZSet score，缺失按 0） */
    private Double score(String schoolName) {
        Double value = redis.opsForZSet().score(HOT_ZSET, schoolName);
        return value == null ? 0d : value;
    }

    private String viewKey(String schoolName) {
        return "postgraduate:heat:views:" + schoolName;
    }

    private String followKey(String schoolName) {
        return "postgraduate:heat:follows:" + schoolName;
    }

    /** 当日明细集合键：member=院校名称:动作，按天隔离，TTL 2 天 */
    private String seenKey(Integer userId) {
        return "postgraduate:heat:seen:" + userId + ":" + LocalDate.now();
    }

    /** 某院校的粉丝集合，member=用户 ID */
    private String followersKey(String schoolName) {
        return "postgraduate:heat:followers:" + schoolName;
    }

    /** 某用户关注的院校集合，member=院校名称 */
    private String followingKey(Integer userId) {
        return "postgraduate:heat:following:" + userId;
    }

    /** 关注人数 = 粉丝集合元素数 */
    private long followerCount(String schoolName) {
        Long count = redis.opsForSet().size(followersKey(schoolName));
        return count == null ? 0 : count;
    }

    /** 组装榜单条目，浏览 / 关注量取消费端累计的历史计数器 */
    private SchoolHeatVO toVO(String schoolName, Integer schoolId, Double heat) {
        SchoolHeatVO vo = new SchoolHeatVO();
        vo.setSchoolId(schoolId);
        vo.setSchoolName(schoolName);
        vo.setHeat(heat);
        vo.setViews(readLong(viewKey(schoolName), 0));
        vo.setFollows(readLong(followKey(schoolName), 0));
        return vo;
    }

    private long readLong(String key, long defaultValue) {
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
