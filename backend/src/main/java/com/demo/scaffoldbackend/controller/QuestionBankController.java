package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.ExamPaperDTO;
import com.demo.scaffoldbackend.dto.ExamQuestionDTO;
import com.demo.scaffoldbackend.dto.QuestionContentDTO;
import com.demo.scaffoldbackend.vo.ExamPaperVO;
import com.demo.scaffoldbackend.vo.ExamQuestionVO;
import com.demo.scaffoldbackend.vo.QuestionContentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库模块
 */
@RestController
@RequestMapping("/api/question-bank")
public class QuestionBankController {

    // ===== 试卷 =====

    /** 分页查询试卷列表（可按科目过滤） */
    @GetMapping("/paper")
    public Result<PageResult<ExamPaperVO>> listPapers(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      @RequestParam(required = false) String examSubject) {
        return null;
    }

    /** 根据科目查询试卷列表 */
    @GetMapping("/paper/subject/{examSubject}")
    public Result<List<ExamPaperVO>> listPapersBySubject(@PathVariable String examSubject) {
        return null;
    }

    /** 根据ID查询试卷 */
    @GetMapping("/paper/{id}")
    public Result<ExamPaperVO> getPaperById(@PathVariable Integer id) {
        return null;
    }

    /** 新增试卷 */
    @PostMapping("/paper")
    public Result<ExamPaperVO> createPaper(@RequestBody ExamPaperDTO paperDTO) {
        return null;
    }

    /** 更新试卷 */
    @PutMapping("/paper/{id}")
    public Result<ExamPaperVO> updatePaper(@PathVariable Integer id, @RequestBody ExamPaperDTO paperDTO) {
        return null;
    }

    /** 删除试卷 */
    @DeleteMapping("/paper/{id}")
    public Result<Boolean> deletePaper(@PathVariable Integer id) {
        return null;
    }

    // ===== 题目 =====

    /** 根据试卷ID查询题目列表 */
    @GetMapping("/question/paper/{paperId}")
    public Result<List<ExamQuestionVO>> listQuestionsByPaperId(@PathVariable Integer paperId) {
        return null;
    }

    /** 分页查询题目列表 */
    @GetMapping("/question")
    public Result<PageResult<ExamQuestionVO>> listQuestions(@RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询题目 */
    @GetMapping("/question/{id}")
    public Result<ExamQuestionVO> getQuestionById(@PathVariable Integer id) {
        return null;
    }

    /** 新增题目 */
    @PostMapping("/question")
    public Result<ExamQuestionVO> createQuestion(@RequestBody ExamQuestionDTO questionDTO) {
        return null;
    }

    /** 更新题目 */
    @PutMapping("/question/{id}")
    public Result<ExamQuestionVO> updateQuestion(@PathVariable Integer id, @RequestBody ExamQuestionDTO questionDTO) {
        return null;
    }

    /** 删除题目 */
    @DeleteMapping("/question/{id}")
    public Result<Boolean> deleteQuestion(@PathVariable Integer id) {
        return null;
    }

    // ===== 题目内容 =====

    /** 根据ID查询题目内容 */
    @GetMapping("/content/{id}")
    public Result<QuestionContentVO> getContentById(@PathVariable Integer id) {
        return null;
    }

    /** 新增题目内容 */
    @PostMapping("/content")
    public Result<QuestionContentVO> createContent(@RequestBody QuestionContentDTO contentDTO) {
        return null;
    }

    /** 更新题目内容 */
    @PutMapping("/content/{id}")
    public Result<QuestionContentVO> updateContent(@PathVariable Integer id, @RequestBody QuestionContentDTO contentDTO) {
        return null;
    }

    /** 删除题目内容 */
    @DeleteMapping("/content/{id}")
    public Result<Boolean> deleteContent(@PathVariable Integer id) {
        return null;
    }
}
