package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.PolicyNoticeDTO;
import com.demo.scaffoldbackend.vo.PolicyNoticeVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 政策通知模块
 */
@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    /** 分页查询政策通知列表（可按类型过滤） */
    @GetMapping
    public Result<PageResult<PolicyNoticeVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) String noticeType) {
        return null;
    }

    /** 根据类型查询政策通知列表 */
    @GetMapping("/type/{noticeType}")
    public Result<List<PolicyNoticeVO>> listByType(@PathVariable String noticeType) {
        return null;
    }

    /** 根据ID查询政策通知 */
    @GetMapping("/{id}")
    public Result<PolicyNoticeVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增政策通知 */
    @PostMapping
    public Result<PolicyNoticeVO> create(@RequestBody PolicyNoticeDTO noticeDTO) {
        return null;
    }

    /** 更新政策通知 */
    @PutMapping("/{id}")
    public Result<PolicyNoticeVO> update(@PathVariable Integer id, @RequestBody PolicyNoticeDTO noticeDTO) {
        return null;
    }

    /** 删除政策通知 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }
}
