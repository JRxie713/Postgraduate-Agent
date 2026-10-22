package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.WrongQuestionDTO;
import com.demo.scaffoldbackend.vo.WrongQuestionVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  错题模块
 */
@RestController
@RequestMapping("/api/wrong-question")
public class WrongQuestionController {

    /** 根据用户ID查询错题列表 */
    @GetMapping("/user/{userId}")
    public Result<List<WrongQuestionVO>> listByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 分页查询错题列表 */
    @GetMapping
    public Result<PageResult<WrongQuestionVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询错题 */
    @GetMapping("/{id}")
    public Result<WrongQuestionVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增错题 */
    @PostMapping
    public Result<WrongQuestionVO> create(@RequestBody WrongQuestionDTO wrongQuestionDTO) {
        return null;
    }

    /** 更新错题 */
    @PutMapping("/{id}")
    public Result<WrongQuestionVO> update(@PathVariable Integer id, @RequestBody WrongQuestionDTO wrongQuestionDTO) {
        return null;
    }

    /** 复习错题（累加复习次数） */
    @PutMapping("/{id}/review")
    public Result<WrongQuestionVO> review(@PathVariable Integer id) {
        return null;
    }

    /** 删除错题 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }
}
