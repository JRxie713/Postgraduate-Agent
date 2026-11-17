package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.RbacGroupDTO;
import com.demo.scaffoldbackend.vo.MyPermissionsVO;
import com.demo.scaffoldbackend.vo.PermissionModuleVO;
import com.demo.scaffoldbackend.vo.RbacGroupVO;

import java.util.List;
import java.util.Set;

/**
 * 禅道式 RBAC：权限组、组权限、用户分组
 */
public interface RbacService {

    /** 计算某个用户拥有的权限点（module:method），命中 Redis 缓存 */
    Set<String> permissionsOf(Integer userId, String userRole);

    /** 当前登录用户的角色与权限点 */
    MyPermissionsVO myPermissions(Integer userId);

    /** 权限点目录（按模块分组） */
    List<PermissionModuleVO> catalog();

    List<RbacGroupVO> listGroups();

    RbacGroupVO saveGroup(RbacGroupDTO dto, Integer id);

    void deleteGroup(Integer id);

    List<Integer> userGroupIds(Integer userId);

    void saveUserGroups(Integer userId, List<Integer> groupIds);

    /** 按角色把新用户加入默认权限组（学生组 / 教师组 / 管理员组） */
    void assignDefaultGroup(Integer userId, String userRole);

    /** 清空某个用户的权限缓存（角色变化、状态变化后调用） */
    void evictPermissionCache(Integer userId);

    /** 清空所有用户的权限缓存（组或组权限变动后调用） */
    void evictAllPermissionCache();
}
