package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.WrongMasteryDTO;
import com.demo.scaffoldbackend.service.WrongBookService;
import com.demo.scaffoldbackend.vo.WrongBookQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 错题本模块
 *
 * <p>错题由日常练习答错自动收录；支持按科目、知识点、掌握状态筛选，标记已掌握与删除。</p>
 */
@RestController
@RequestMapping("/api/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongBookService wrongBookService;

    /** 分页查询我的错题 */
    @GetMapping
    public Result<PageResult<WrongBookQuestionVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String examSubject,
                                                        @RequestParam(required = false) String knowledgePoint,
                                                        @RequestParam(required = false) String masteryLevel) {
        return Result.success(wrongBookService.pageWrongQuestions(currentUserId(), page, size,
                examSubject, knowledgePoint, masteryLevel));
    }

    /** 错题详情 */
    @GetMapping("/{id}")
    public Result<WrongBookQuestionVO> detail(@PathVariable Integer id) {
        return Result.success(wrongBookService.getWrongQuestion(currentUserId(), id));
    }

    /** 标记已掌握 / 仍不会 */
    @PutMapping("/{id}/mastery")
    public Result<WrongBookQuestionVO> updateMastery(@PathVariable Integer id,
                                                     @RequestBody WrongMasteryDTO dto) {
        return Result.success(wrongBookService.updateMastery(currentUserId(), id,
                dto == null ? null : dto.getMasteryLevel()));
    }

    /** 删除错题 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(wrongBookService.deleteWrongQuestion(currentUserId(), id));
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
