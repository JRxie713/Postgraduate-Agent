package com.demo.scaffoldbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.PolicyNoticeDTO;
import com.demo.scaffoldbackend.entity.PolicyNotice;
import com.demo.scaffoldbackend.mapper.PolicyNoticeMapper;
import com.demo.scaffoldbackend.vo.PolicyNoticeVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资讯与政策提醒模块（F11）
 */
@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final PolicyNoticeMapper policyNoticeMapper;

    public PolicyController(PolicyNoticeMapper policyNoticeMapper) {
        this.policyNoticeMapper = policyNoticeMapper;
    }

    /** 分页查询政策通知列表（可按类型过滤） */
    @PreAuthorize("hasAuthority('policy:view')")
    @GetMapping
    public Result<PageResult<PolicyNoticeVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) String noticeType) {
        LambdaQueryWrapper<PolicyNotice> query = new LambdaQueryWrapper<>();
        if (noticeType != null && !noticeType.isBlank()) {
            query.eq(PolicyNotice::getNoticeType, noticeType);
        }
        query.orderByDesc(PolicyNotice::getEffectTime).orderByDesc(PolicyNotice::getId);
        Page<PolicyNotice> result = policyNoticeMapper.selectPage(new Page<>(page, size), query);
        List<PolicyNoticeVO> records = new ArrayList<>();
        for (PolicyNotice notice : result.getRecords()) {
            records.add(toVO(notice));
        }
        return Result.success(PageResult.of(result.getTotal(), page, size, records));
    }

    /** 根据类型查询政策通知列表 */
    @PreAuthorize("hasAuthority('policy:view')")
    @GetMapping("/type/{noticeType}")
    public Result<List<PolicyNoticeVO>> listByType(@PathVariable String noticeType) {
        List<PolicyNotice> notices = policyNoticeMapper.selectList(new LambdaQueryWrapper<PolicyNotice>()
                .eq(PolicyNotice::getNoticeType, noticeType)
                .orderByDesc(PolicyNotice::getEffectTime)
                .orderByDesc(PolicyNotice::getId));
        List<PolicyNoticeVO> vos = new ArrayList<>();
        for (PolicyNotice notice : notices) {
            vos.add(toVO(notice));
        }
        return Result.success(vos);
    }

    /** 根据ID查询政策通知 */
    @PreAuthorize("hasAuthority('policy:view')")
    @GetMapping("/{id}")
    public Result<PolicyNoticeVO> getById(@PathVariable Integer id) {
        PolicyNotice notice = policyNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "通知不存在");
        }
        return Result.success(toVO(notice));
    }

    /** 新增政策通知（需要 policy:create 权限） */
    @PreAuthorize("hasAuthority('policy:create')")
    @PostMapping
    public Result<PolicyNoticeVO> create(@RequestBody PolicyNoticeDTO dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BusinessException(400, "通知标题不能为空");
        }
        PolicyNotice notice = new PolicyNotice();
        notice.setTitle(dto.getTitle().trim());
        notice.setNoticeType(dto.getNoticeType());
        notice.setNoticeSource(dto.getNoticeSource());
        notice.setEffectTime(dto.getEffectTime());
        notice.setSubTags(dto.getSubTags());
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        policyNoticeMapper.insert(notice);
        return Result.success(toVO(notice));
    }

    /** 更新政策通知（需要 policy:update 权限） */
    @PreAuthorize("hasAuthority('policy:update')")
    @PutMapping("/{id}")
    public Result<PolicyNoticeVO> update(@PathVariable Integer id, @RequestBody PolicyNoticeDTO dto) {
        PolicyNotice notice = policyNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "通知不存在");
        }
        if (dto != null) {
            if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                notice.setTitle(dto.getTitle().trim());
            }
            if (dto.getNoticeType() != null) {
                notice.setNoticeType(dto.getNoticeType());
            }
            if (dto.getNoticeSource() != null) {
                notice.setNoticeSource(dto.getNoticeSource());
            }
            if (dto.getEffectTime() != null) {
                notice.setEffectTime(dto.getEffectTime());
            }
            if (dto.getSubTags() != null) {
                notice.setSubTags(dto.getSubTags());
            }
        }
        notice.setUpdateTime(LocalDateTime.now());
        policyNoticeMapper.updateById(notice);
        return Result.success(toVO(notice));
    }

    /** 删除政策通知（需要 policy:delete 权限） */
    @PreAuthorize("hasAuthority('policy:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        policyNoticeMapper.deleteById(id);
        return Result.success(true);
    }

    private PolicyNoticeVO toVO(PolicyNotice notice) {
        PolicyNoticeVO vo = new PolicyNoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setNoticeType(notice.getNoticeType());
        vo.setNoticeSource(notice.getNoticeSource());
        vo.setEffectTime(notice.getEffectTime());
        vo.setSubTags(notice.getSubTags());
        vo.setCreateTime(notice.getCreateTime());
        vo.setUpdateTime(notice.getUpdateTime());
        return vo;
    }
}
