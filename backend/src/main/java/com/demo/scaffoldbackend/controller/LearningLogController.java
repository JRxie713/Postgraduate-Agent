package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.LearningLogDTO;
import com.demo.scaffoldbackend.service.LearningLogService;
import com.demo.scaffoldbackend.vo.LearningLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  学习日志模块
 */
@RestController
@RequestMapping("/api/learning-log")
@RequiredArgsConstructor
public class LearningLogController {

    private final LearningLogService learningLogService;

    /** 根据用户ID查询学习日志列表 */
    @GetMapping("/user/{userId}")
    public Result<List<LearningLogVO>> listByUserId(@PathVariable Integer userId) {
        return Result.success(learningLogService.listByUserId(userId));
    }

    /** 打卡：同一用户同一天重复打卡时更新原记录 */
    @PostMapping("/check-in")
    public Result<LearningLogVO> checkIn(@RequestBody LearningLogDTO logDTO) {
        return Result.success(learningLogService.checkIn(logDTO));
    }

    /** 分页查询学习日志列表 */
    @GetMapping
    public Result<PageResult<LearningLogVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(learningLogService.pageLogs(page, size));
    }

    /** 根据ID查询学习日志 */
    @GetMapping("/{id}")
    public Result<LearningLogVO> getById(@PathVariable Integer id) {
        return Result.success(learningLogService.getLogById(id));
    }

    /** 更新学习日志 */
    @PutMapping("/{id}")
    public Result<LearningLogVO> update(@PathVariable Integer id, @RequestBody LearningLogDTO logDTO) {
        return Result.success(learningLogService.updateLog(id, logDTO));
    }

    /** 删除学习日志 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(learningLogService.deleteLog(id));
    }
}
