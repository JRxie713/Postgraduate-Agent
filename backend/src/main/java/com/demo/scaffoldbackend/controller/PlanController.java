package com.demo.scaffoldbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.PlanTaskDTO;
import com.demo.scaffoldbackend.dto.PlanTaskStatusDTO;
import com.demo.scaffoldbackend.dto.StudyPlanDTO;
import com.demo.scaffoldbackend.entity.PlanTask;
import com.demo.scaffoldbackend.entity.StudyPlan;
import com.demo.scaffoldbackend.mapper.PlanTaskMapper;
import com.demo.scaffoldbackend.mapper.StudyPlanMapper;
import com.demo.scaffoldbackend.vo.PlanTaskVO;
import com.demo.scaffoldbackend.vo.StudyPlanVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习计划模块（个性化学习规划 F7）
 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final StudyPlanMapper planMapper;
    private final PlanTaskMapper taskMapper;
    private final StringRedisTemplate redis;

    public PlanController(StudyPlanMapper planMapper, PlanTaskMapper taskMapper, StringRedisTemplate redis) {
        this.planMapper = planMapper;
        this.taskMapper = taskMapper;
        this.redis = redis;
    }

    /** 当前登录用户的学习计划（含任务与完成进度） */
    @GetMapping("/mine")
    public Result<List<StudyPlanVO>> mine() {
        List<StudyPlan> plans = planMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, currentUserId())
                .orderByDesc(StudyPlan::getCreateTime));
        List<StudyPlanVO> vos = new ArrayList<>();
        for (StudyPlan plan : plans) {
            vos.add(toPlanVO(plan));
        }
        return Result.success(vos);
    }

    /** 新建学习计划 */
    @PostMapping
    public Result<StudyPlanVO> create(@RequestBody StudyPlanDTO dto) {
        if (dto == null || isBlank(dto.getPlanCycle())) {
            throw new BusinessException(400, "请选择计划周期");
        }
        StudyPlan plan = new StudyPlan();
        plan.setUserId(currentUserId());
        plan.setPlanCycle(dto.getPlanCycle().trim());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setPlanStatus("ACTIVE");
        plan.setCompletionRate(0.0);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.insert(plan);
        return Result.success(toPlanVO(plan));
    }

    /** 删除自己的学习计划（任务一并删除） */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        StudyPlan plan = requireOwnPlan(id);
        taskMapper.delete(new LambdaQueryWrapper<PlanTask>().eq(PlanTask::getPlanId, plan.getId()));
        planMapper.deleteById(plan.getId());
        return Result.success(true);
    }

    /** 给计划添加任务 */
    @PostMapping("/task")
    public Result<PlanTaskVO> createTask(@RequestBody PlanTaskDTO dto) {
        if (dto == null || dto.getPlanId() == null) {
            throw new BusinessException(400, "请选择所属计划");
        }
        if (isBlank(dto.getTaskName())) {
            throw new BusinessException(400, "任务名称不能为空");
        }
        StudyPlan plan = requireOwnPlan(dto.getPlanId());
        PlanTask task = new PlanTask();
        task.setPlanId(plan.getId());
        task.setTaskName(dto.getTaskName().trim());
        task.setTaskSubject(isBlank(dto.getTaskSubject()) ? "综合" : dto.getTaskSubject().trim());
        task.setChapter(dto.getChapter());
        task.setDueDate(dto.getDueDate());
        task.setTaskStatus("TODO");
        task.setIsTimeout(false);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        refreshPlanProgress(plan);
        return Result.success(toTaskVO(task));
    }

    /** 勾选 / 取消勾选任务完成 */
    @PutMapping("/task/{id}/status")
    public Result<PlanTaskVO> updateTaskStatus(@PathVariable Integer id, @RequestBody PlanTaskStatusDTO dto) {
        PlanTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        StudyPlan plan = requireOwnPlan(task.getPlanId());
        String status = dto == null || dto.getTaskStatus() == null ? "" : dto.getTaskStatus().trim().toUpperCase();
        if (!"DONE".equals(status) && !"TODO".equals(status)) {
            throw new BusinessException(400, "任务状态只能是 TODO 或 DONE");
        }
        boolean completedNow = "DONE".equals(status) && !"DONE".equals(task.getTaskStatus());
        task.setTaskStatus(status);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        refreshPlanProgress(plan);

        if (completedNow) {
            String activityKey = "postgraduate:activity";
            String activity = LocalDateTime.now() + " · 用户完成任务：「" + task.getTaskName() + "」";
            redis.opsForList().leftPush(activityKey, activity);
            redis.opsForList().trim(activityKey, 0, 99);
            redis.expire(activityKey, Duration.ofDays(7));
        }
        return Result.success(toTaskVO(task));
    }

    /** 删除任务 */
    @DeleteMapping("/task/{id}")
    public Result<Boolean> deleteTask(@PathVariable Integer id) {
        PlanTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        StudyPlan plan = requireOwnPlan(task.getPlanId());
        taskMapper.deleteById(task.getId());
        refreshPlanProgress(plan);
        return Result.success(true);
    }

    // ===== 内部方法 =====

    private Integer currentUserId() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return Integer.valueOf(name);
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
    }

    private StudyPlan requireOwnPlan(Integer planId) {
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "学习计划不存在");
        }
        if (!currentUserId().equals(plan.getUserId())) {
            throw new BusinessException(403, "无权操作他人的学习计划");
        }
        return plan;
    }

    /** 根据任务完成情况回写计划完成率与状态 */
    private void refreshPlanProgress(StudyPlan plan) {
        List<PlanTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<PlanTask>().eq(PlanTask::getPlanId, plan.getId()));
        long done = tasks.stream().filter(t -> "DONE".equals(t.getTaskStatus())).count();
        double rate = tasks.isEmpty() ? 0.0 : Math.round(done * 1000.0 / tasks.size()) / 10.0;
        plan.setCompletionRate(rate);
        plan.setPlanStatus(!tasks.isEmpty() && done == tasks.size() ? "COMPLETED" : "ACTIVE");
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);
    }

    private StudyPlanVO toPlanVO(StudyPlan plan) {
        StudyPlanVO vo = new StudyPlanVO();
        vo.setId(plan.getId());
        vo.setUserId(plan.getUserId());
        vo.setPlanCycle(plan.getPlanCycle());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setPlanStatus(plan.getPlanStatus());
        vo.setCompletionRate(plan.getCompletionRate());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());
        List<PlanTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<PlanTask>()
                .eq(PlanTask::getPlanId, plan.getId())
                .orderByAsc(PlanTask::getDueDate)
                .orderByAsc(PlanTask::getId));
        List<PlanTaskVO> taskVOs = new ArrayList<>();
        int done = 0;
        for (PlanTask task : tasks) {
            if ("DONE".equals(task.getTaskStatus())) {
                done++;
            }
            taskVOs.add(toTaskVO(task));
        }
        vo.setTasks(taskVOs);
        vo.setTaskTotal(tasks.size());
        vo.setTaskDone(done);
        return vo;
    }

    private PlanTaskVO toTaskVO(PlanTask task) {
        PlanTaskVO vo = new PlanTaskVO();
        vo.setId(task.getId());
        vo.setPlanId(task.getPlanId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskSubject(task.getTaskSubject());
        vo.setChapter(task.getChapter());
        vo.setDueDate(task.getDueDate());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setIsTimeout(task.getIsTimeout());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
