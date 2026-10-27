package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.PlanTaskDTO;
import com.demo.scaffoldbackend.dto.PlanTaskStatusDTO;
import com.demo.scaffoldbackend.dto.StudyPlanDTO;
import com.demo.scaffoldbackend.vo.PlanTaskVO;
import com.demo.scaffoldbackend.vo.StudyPlanVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习计划模块
 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    // ===== 学习计划 =====

    /** 根据用户ID查询学习计划列表 */
    @GetMapping("/user/{userId}")
    public Result<List<StudyPlanVO>> listByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 分页查询学习计划列表 */
    @GetMapping
    public Result<PageResult<StudyPlanVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询学习计划 */
    @GetMapping("/{id}")
    public Result<StudyPlanVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增学习计划 */
    @PostMapping
    public Result<StudyPlanVO> create(@RequestBody StudyPlanDTO planDTO) {
        return null;
    }

    /** 更新学习计划 */
    @PutMapping("/{id}")
    public Result<StudyPlanVO> update(@PathVariable Integer id, @RequestBody StudyPlanDTO planDTO) {
        return null;
    }

    /** 删除学习计划 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }

    // ===== 计划任务 =====

    /** 根据计划ID查询任务列表 */
    @GetMapping("/task/plan/{planId}")
    public Result<List<PlanTaskVO>> listTasksByPlanId(@PathVariable Integer planId) {
        return null;
    }

    /** 分页查询任务列表 */
    @GetMapping("/task")
    public Result<PageResult<PlanTaskVO>> listTasks(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询任务 */
    @GetMapping("/task/{id}")
    public Result<PlanTaskVO> getTaskById(@PathVariable Integer id) {
        return null;
    }

    /** 新增任务 */
    @PostMapping("/task")
    public Result<PlanTaskVO> createTask(@RequestBody PlanTaskDTO taskDTO) {
        return null;
    }

    /** 更新任务 */
    @PutMapping("/task/{id}")
    public Result<PlanTaskVO> updateTask(@PathVariable Integer id, @RequestBody PlanTaskDTO taskDTO) {
        return null;
    }

    /** 更新任务状态 */
    @PutMapping("/task/{id}/status")
    public Result<PlanTaskVO> updateTaskStatus(@PathVariable Integer id, @RequestBody PlanTaskStatusDTO statusDTO) {
        return null;
    }

    /** 删除任务 */
    @DeleteMapping("/task/{id}")
    public Result<Boolean> deleteTask(@PathVariable Integer id) {
        return null;
    }
}
