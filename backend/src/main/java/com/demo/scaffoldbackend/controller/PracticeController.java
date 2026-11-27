package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.PracticeAnswerDTO;
import com.demo.scaffoldbackend.service.PracticeService;
import com.demo.scaffoldbackend.vo.PracticeAnswerResultVO;
import com.demo.scaffoldbackend.vo.PracticeOptionsVO;
import com.demo.scaffoldbackend.vo.PracticeQuestionVO;
import com.demo.scaffoldbackend.vo.PracticeRecordVO;
import com.demo.scaffoldbackend.vo.PracticeSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 刷题训练模块
 *
 * <p>日常练习：筛选题目 → 提交答案判题 → 答错自动收录错题 → 专项练习同知识点题目。</p>
 */
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    /** 分页查询练习题（科目 / 章节 / 知识点 / 难度 / 关键词） */
    @GetMapping("/questions")
    public Result<PageResult<PracticeQuestionVO>> questions(@RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer size,
                                                            @RequestParam(required = false) String examSubject,
                                                            @RequestParam(required = false) String chapter,
                                                            @RequestParam(required = false) String knowledgePoint,
                                                            @RequestParam(required = false) String difficulty,
                                                            @RequestParam(required = false) String keyword) {
        return Result.success(practiceService.pageQuestions(currentUserId(), page, size,
                examSubject, chapter, knowledgePoint, difficulty, keyword));
    }

    /** 查询练习题详情 */
    @GetMapping("/questions/{id}")
    public Result<PracticeQuestionVO> questionDetail(@PathVariable Integer id) {
        return Result.success(practiceService.getQuestion(currentUserId(), id));
    }

    /** 提交客观题答案，返回判题结果、正确答案与解析 */
    @PostMapping("/answers")
    public Result<PracticeAnswerResultVO> submitAnswer(@RequestBody PracticeAnswerDTO dto) {
        return Result.success(practiceService.submitAnswer(currentUserId(), dto));
    }

    /** 分页查询日常练习记录 */
    @GetMapping("/records")
    public Result<PageResult<PracticeRecordVO>> records(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String examSubject,
                                                        @RequestParam(required = false) String knowledgePoint,
                                                        @RequestParam(required = false) Boolean onlyWrong) {
        return Result.success(practiceService.pageRecords(currentUserId(), page, size,
                examSubject, knowledgePoint, onlyWrong));
    }

    /** 练习概览：正确率、今日练习、错题与掌握情况 */
    @GetMapping("/summary")
    public Result<PracticeSummaryVO> summary() {
        return Result.success(practiceService.summary(currentUserId()));
    }

    /** 同知识点同类题（专项练习） */
    @GetMapping("/similar/{questionId}")
    public Result<List<PracticeQuestionVO>> similar(@PathVariable Integer questionId,
                                                    @RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(practiceService.similarQuestions(currentUserId(), questionId, limit));
    }

    /** 每日一练：自定义题量随机抽题 */
    @GetMapping("/daily")
    public Result<List<PracticeQuestionVO>> daily(@RequestParam(defaultValue = "10") Integer count,
                                                  @RequestParam(required = false) String examSubject,
                                                  @RequestParam(required = false) String difficulty) {
        return Result.success(practiceService.dailyQuestions(currentUserId(), count, examSubject, difficulty));
    }

    /** 筛选项：科目、章节、知识点、难度 */
    @GetMapping("/options")
    public Result<PracticeOptionsVO> options() {
        return Result.success(practiceService.options());
    }

    private Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(401, "未登录或登录已失效");
        }
        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "登录状态异常，请重新登录");
        }
    }
}
