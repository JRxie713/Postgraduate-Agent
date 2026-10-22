package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.vo.MockExamStatVO;
import com.demo.scaffoldbackend.vo.QuestionQualityVO;
import com.demo.scaffoldbackend.vo.SchoolHotVO;
import com.demo.scaffoldbackend.vo.StatDistributionVO;
import com.demo.scaffoldbackend.vo.StatOverviewVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据统计模块
 */
@RestController
@RequestMapping("/api/stat")
public class StatController {

    /** 平台运营概览 */
    @GetMapping("/overview")
    public Result<StatOverviewVO> overview(@RequestParam(defaultValue = "WEEK") String range) {
        return null;
    }

    /** 学情分布统计 */
    @GetMapping("/learning-distribution")
    public Result<StatDistributionVO> learningDistribution(@RequestParam(defaultValue = "subject") String groupBy,
                                                           @RequestParam(required = false) Integer examYear) {
        return null;
    }

    /** 题库质量统计 */
    @GetMapping("/question-quality")
    public Result<PageResult<QuestionQualityVO>> questionQuality(@RequestParam(required = false) String examSubject,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 模考成绩分布 */
    @GetMapping("/mock-exam/{paperId}")
    public Result<MockExamStatVO> mockExamStat(@PathVariable Integer paperId,
                                               @RequestParam(required = false) Integer examYear) {
        return null;
    }

    /** 择校热度榜 */
    @GetMapping("/school-hot")
    public Result<List<SchoolHotVO>> schoolHot(@RequestParam(required = false) String region,
                                               @RequestParam(required = false) String major,
                                               @RequestParam(defaultValue = "10") Integer limit) {
        return null;
    }
}
