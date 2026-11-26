package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.SchoolHeatDTO;
import com.demo.scaffoldbackend.service.SchoolHeatService;
import com.demo.scaffoldbackend.vo.SchoolCardVO;
import com.demo.scaffoldbackend.vo.SchoolHeatVO;
import com.demo.scaffoldbackend.vo.SchoolOptionVO;
import com.demo.scaffoldbackend.vo.SchoolTrackVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 院校热度模块（Redis + Kafka 协同）
 *
 * <p>接口前缀：/api/school-heat</p>
 */
@RestController
@RequestMapping("/api/school-heat")
public class SchoolHeatController {

    private final SchoolHeatService schoolHeatService;

    public SchoolHeatController(SchoolHeatService schoolHeatService) {
        this.schoolHeatService = schoolHeatService;
    }

    /** 上报一次院校浏览 / 关注（Redis 实时去重计数 + 投递 Kafka 异步聚合） */
    @PostMapping
    public Result<SchoolHeatVO> record(@Valid @RequestBody SchoolHeatDTO dto) {
        return Result.success(schoolHeatService.record(dto));
    }

    /** 院校热度排行榜，默认前 5 */
    @GetMapping("/rank")
    public Result<List<SchoolHeatVO>> rank(@RequestParam(defaultValue = "5") int top) {
        return Result.success(schoolHeatService.rank(top));
    }

    /** 可选院校列表（择校页埋点下拉用） */
    @GetMapping("/options")
    public Result<List<SchoolOptionVO>> options() {
        return Result.success(schoolHeatService.options());
    }

    /** 择校面板卡片：关注人数 + 当前用户是否已关注 */
    @GetMapping("/cards")
    public Result<List<SchoolCardVO>> cards(@RequestParam(required = false) Integer userId) {
        return Result.success(schoolHeatService.cards(userId));
    }

    /** 当前用户今日浏览 / 关注过的院校明细 */
    @GetMapping("/mine")
    public Result<List<SchoolTrackVO>> mine(@RequestParam Integer userId) {
        return Result.success(schoolHeatService.myTracks(userId));
    }
}
