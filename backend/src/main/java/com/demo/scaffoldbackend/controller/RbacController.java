package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.RbacGroupDTO;
import com.demo.scaffoldbackend.dto.UserGroupDTO;
import com.demo.scaffoldbackend.service.RbacService;
import com.demo.scaffoldbackend.vo.MyPermissionsVO;
import com.demo.scaffoldbackend.vo.PermissionModuleVO;
import com.demo.scaffoldbackend.vo.RbacGroupVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 禅道式 RBAC 管理接口：权限组、组权限、用户分组
 */
@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    /** 当前登录用户的角色和权限点，前端据此显示菜单和按钮 */
    @GetMapping("/me")
    public Result<MyPermissionsVO> me(Authentication authentication) {
        Integer userId = Integer.valueOf(authentication.getName());
        return Result.success(rbacService.myPermissions(userId));
    }

    /** 权限点目录，用于渲染勾选框 */
    @PreAuthorize("hasAuthority('rbac:view')")
    @GetMapping("/permissions")
    public Result<List<PermissionModuleVO>> permissions() {
        return Result.success(rbacService.catalog());
    }

    /** 权限组列表（含已勾选权限） */
    @PreAuthorize("hasAuthority('rbac:view')")
    @GetMapping("/groups")
    public Result<List<RbacGroupVO>> groups() {
        return Result.success(rbacService.listGroups());
    }

    /** 新建权限组 */
    @PreAuthorize("hasAuthority('rbac:manage')")
    @PostMapping("/groups")
    public Result<RbacGroupVO> create(@RequestBody RbacGroupDTO dto) {
        return Result.success(rbacService.saveGroup(dto, null));
    }

    /** 修改权限组（含重新勾选权限点） */
    @PreAuthorize("hasAuthority('rbac:manage')")
    @PutMapping("/groups/{id}")
    public Result<RbacGroupVO> update(@PathVariable Integer id, @RequestBody RbacGroupDTO dto) {
        return Result.success(rbacService.saveGroup(dto, id));
    }

    /** 删除权限组 */
    @PreAuthorize("hasAuthority('rbac:manage')")
    @DeleteMapping("/groups/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        rbacService.deleteGroup(id);
        return Result.success(true);
    }

    /** 查询某个用户所属的权限组 */
    @PreAuthorize("hasAuthority('rbac:view')")
    @GetMapping("/users/{userId}/groups")
    public Result<List<Integer>> userGroups(@PathVariable Integer userId) {
        return Result.success(rbacService.userGroupIds(userId));
    }

    /** 给用户分配权限组（全量覆盖） */
    @PreAuthorize("hasAuthority('rbac:manage')")
    @PutMapping("/users/{userId}/groups")
    public Result<Boolean> saveUserGroups(@PathVariable Integer userId, @RequestBody UserGroupDTO dto) {
        rbacService.saveUserGroups(userId, dto == null ? null : dto.getGroupIds());
        return Result.success(true);
    }
}
