package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.InterviewRecordDTO;
import com.demo.scaffoldbackend.vo.InterviewRecordVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复试训练模块
 */
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    /** 根据用户ID查询复试训练记录列表 */
    @GetMapping("/user/{userId}")
    public Result<List<InterviewRecordVO>> listByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 分页查询复试训练记录列表 */
    @GetMapping
    public Result<PageResult<InterviewRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询复试训练记录 */
    @GetMapping("/{id}")
    public Result<InterviewRecordVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增复试训练记录 */
    @PostMapping
    public Result<InterviewRecordVO> create(@RequestBody InterviewRecordDTO recordDTO) {
        return null;
    }

    /** 更新复试训练记录 */
    @PutMapping("/{id}")
    public Result<InterviewRecordVO> update(@PathVariable Integer id, @RequestBody InterviewRecordDTO recordDTO) {
        return null;
    }

    /** 删除复试训练记录 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }
}
