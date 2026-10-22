package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.ConsultationDTO;
import com.demo.scaffoldbackend.vo.ConsultationVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  答疑会话模块
 */
@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    /** 发起提问 */
    @PostMapping("/ask")
    public Result<ConsultationVO> ask(@RequestBody ConsultationDTO consultationDTO) {
        return null;
    }

    /** 根据会话ID查询一轮对话记录 */
    @GetMapping("/session/{sessionId}")
    public Result<List<ConsultationVO>> listBySessionId(@PathVariable String sessionId) {
        return null;
    }

    /** 根据用户ID查询答疑记录 */
    @GetMapping("/user/{userId}")
    public Result<List<ConsultationVO>> listByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 分页查询答疑记录列表 */
    @GetMapping
    public Result<PageResult<ConsultationVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询答疑记录 */
    @GetMapping("/{id}")
    public Result<ConsultationVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 更新答疑记录 */
    @PutMapping("/{id}")
    public Result<ConsultationVO> update(@PathVariable Integer id, @RequestBody ConsultationDTO consultationDTO) {
        return null;
    }

    /** 删除答疑记录 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }
}
