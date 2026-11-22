package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.StudyCheckinDTO;
import com.demo.scaffoldbackend.service.StudyCheckinService;
import com.demo.scaffoldbackend.vo.StudyCheckinVO;
import com.demo.scaffoldbackend.vo.StudyOverviewVO;
import com.demo.scaffoldbackend.vo.StudyRankVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习打卡模块（Redis + Kafka 协同）
 *
 * <p>接口前缀：/api/study-checkin</p>
 */
@RestController
@RequestMapping("/api/study-checkin")
public class StudyCheckinController {

    private final StudyCheckinService studyCheckinService;

    public StudyCheckinController(StudyCheckinService studyCheckinService) {
        this.studyCheckinService = studyCheckinService;
    }

    /** 每日学习打卡（Redis 实时计数 + 投递 Kafka 事件异步聚合） */
    @PostMapping
    public Result<StudyCheckinVO> checkIn(@Valid @RequestBody StudyCheckinDTO dto) {
        return Result.success(studyCheckinService.checkIn(dto));
    }

    /** 查询个人学习概览（Hash 缓存读穿透） */
    @GetMapping("/overview/{userId}")
    public Result<StudyOverviewVO> overview(@PathVariable Integer userId) {
        return Result.success(studyCheckinService.overview(userId));
    }

    /** 连续打卡天数排行榜，默认前 10 */
    @GetMapping("/rank/streak")
    public Result<List<StudyRankVO>> streakRank(@RequestParam(defaultValue = "10") int top) {
        return Result.success(studyCheckinService.streakRank(top));
    }

    /** 今日学习时长排行榜，默认前 10 */
    @GetMapping("/rank/today")
    public Result<List<StudyRankVO>> todayRank(@RequestParam(defaultValue = "10") int top) {
        return Result.success(studyCheckinService.todayMinutesRank(top));
    }

    /** 主动失效个人概览缓存（演示缓存更新策略） */
    @DeleteMapping("/cache/{userId}")
    public Result<Boolean> evictCache(@PathVariable Integer userId) {
        studyCheckinService.evictOverviewCache(userId);
        return Result.success(Boolean.TRUE);
    }
}
