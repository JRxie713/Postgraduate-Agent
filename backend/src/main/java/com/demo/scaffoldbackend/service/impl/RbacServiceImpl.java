package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.dto.RbacGroupDTO;
import com.demo.scaffoldbackend.entity.RbacGroup;
import com.demo.scaffoldbackend.entity.RbacGroupPriv;
import com.demo.scaffoldbackend.entity.RbacUserGroup;
import com.demo.scaffoldbackend.entity.User;
import com.demo.scaffoldbackend.mapper.RbacGroupMapper;
import com.demo.scaffoldbackend.mapper.RbacGroupPrivMapper;
import com.demo.scaffoldbackend.mapper.RbacUserGroupMapper;
import com.demo.scaffoldbackend.mapper.UserMapper;
import com.demo.scaffoldbackend.rbac.PermissionCatalog;
import com.demo.scaffoldbackend.service.RbacService;
import com.demo.scaffoldbackend.vo.MyPermissionsVO;
import com.demo.scaffoldbackend.vo.PermissionModuleVO;
import com.demo.scaffoldbackend.vo.RbacGroupVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RbacServiceImpl implements RbacService {

    private static final String PERM_KEY_PREFIX = "auth:perm:";
    private static final long PERM_TTL_MINUTES = 30;

    private final RbacGroupMapper groupMapper;
    private final RbacGroupPrivMapper privMapper;
    private final RbacUserGroupMapper userGroupMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redis;

    public RbacServiceImpl(RbacGroupMapper groupMapper, RbacGroupPrivMapper privMapper,
                           RbacUserGroupMapper userGroupMapper, UserMapper userMapper,
                           StringRedisTemplate redis) {
        this.groupMapper = groupMapper;
        this.privMapper = privMapper;
        this.userGroupMapper = userGroupMapper;
        this.userMapper = userMapper;
        this.redis = redis;
    }

    @Override
    public Set<String> permissionsOf(Integer userId, String userRole) {
        // 禅道里超级管理员不受权限组限制，这里 ADMIN 角色直通
        if ("ADMIN".equals(userRole)) {
            return PermissionCatalog.allKeys();
        }
        String cacheKey = PERM_KEY_PREFIX + userId;
        Set<String> cached = redis.opsForSet().members(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        Set<String> permissions = new LinkedHashSet<>();
        List<RbacUserGroup> links = userGroupMapper.selectList(
                new LambdaQueryWrapper<RbacUserGroup>().eq(RbacUserGroup::getUserId, userId));
        if (!links.isEmpty()) {
            List<Integer> groupIds = new ArrayList<>();
            for (RbacUserGroup link : links) {
                groupIds.add(link.getGroupId());
            }
            List<RbacGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<RbacGroup>()
                    .in(RbacGroup::getId, groupIds)
                    .eq(RbacGroup::getStatus, "NORMAL"));
            List<Integer> enabledIds = new ArrayList<>();
            for (RbacGroup group : groups) {
                enabledIds.add(group.getId());
            }
            if (!enabledIds.isEmpty()) {
                List<RbacGroupPriv> privs = privMapper.selectList(new LambdaQueryWrapper<RbacGroupPriv>()
                        .in(RbacGroupPriv::getGroupId, enabledIds));
                for (RbacGroupPriv priv : privs) {
                    String key = priv.getModule() + ":" + priv.getMethod();
                    if (PermissionCatalog.exists(key)) {
                        permissions.add(key);
                    }
                }
            }
        }

        // 权限为空的用户不写缓存，避免组变更后长期为空
        if (!permissions.isEmpty()) {
            redis.opsForSet().add(cacheKey, permissions.toArray(new String[0]));
            redis.expire(cacheKey, Duration.ofMinutes(PERM_TTL_MINUTES));
        }
        return permissions;
    }

    @Override
    public MyPermissionsVO myPermissions(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        MyPermissionsVO vo = new MyPermissionsVO();
        vo.setUserId(userId);
        vo.setUserRole(user.getUserRole());
        vo.setPermissions(new ArrayList<>(permissionsOf(userId, user.getUserRole())));
        return vo;
    }

    @Override
    public List<PermissionModuleVO> catalog() {
        Map<String, PermissionModuleVO> modules = new LinkedHashMap<>();
        for (PermissionCatalog.Item item : PermissionCatalog.ITEMS) {
            PermissionModuleVO module = modules.computeIfAbsent(item.module(), m -> {
                PermissionModuleVO vo = new PermissionModuleVO();
                vo.setModule(m);
                vo.setModuleLabel(item.moduleLabel());
                return vo;
            });
            module.getMethods().add(new PermissionModuleVO.Method(item.method(), item.methodLabel()));
        }
        return new ArrayList<>(modules.values());
    }

    @Override
    public List<RbacGroupVO> listGroups() {
        List<RbacGroup> groups = groupMapper.selectList(
                new LambdaQueryWrapper<RbacGroup>().orderByAsc(RbacGroup::getId));
        List<RbacGroupVO> result = new ArrayList<>();
        for (RbacGroup group : groups) {
            result.add(toVO(group));
        }
        return result;
    }

    @Override
    public RbacGroupVO saveGroup(RbacGroupDTO dto, Integer id) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException(400, "权限组名称不能为空");
        }
        String name = dto.getName().trim();
        Long sameName = groupMapper.selectCount(new LambdaQueryWrapper<RbacGroup>()
                .eq(RbacGroup::getName, name)
                .ne(id != null, RbacGroup::getId, id));
        if (sameName != null && sameName > 0) {
            throw new BusinessException(409, "权限组名称已存在");
        }

        RbacGroup group;
        if (id == null) {
            group = new RbacGroup();
            group.setCreateTime(LocalDateTime.now());
        } else {
            group = groupMapper.selectById(id);
            if (group == null) {
                throw new BusinessException(404, "权限组不存在");
            }
        }
        group.setName(name);
        group.setDescription(dto.getDescription());
        group.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "NORMAL" : dto.getStatus());
        group.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            groupMapper.insert(group);
        } else {
            groupMapper.updateById(group);
        }

        privMapper.delete(new LambdaQueryWrapper<RbacGroupPriv>().eq(RbacGroupPriv::getGroupId, group.getId()));
        if (dto.getPrivileges() != null) {
            Set<String> saved = new LinkedHashSet<>();
            for (String item : dto.getPrivileges()) {
                if (item == null || !PermissionCatalog.exists(item) || !saved.add(item)) {
                    continue;
                }
                String[] parts = item.split(":");
                RbacGroupPriv priv = new RbacGroupPriv();
                priv.setGroupId(group.getId());
                priv.setModule(parts[0]);
                priv.setMethod(parts[1]);
                privMapper.insert(priv);
            }
        }
        evictAllPermissionCache();
        return toVO(group);
    }

    @Override
    public void deleteGroup(Integer id) {
        groupMapper.deleteById(id);
        privMapper.delete(new LambdaQueryWrapper<RbacGroupPriv>().eq(RbacGroupPriv::getGroupId, id));
        userGroupMapper.delete(new LambdaQueryWrapper<RbacUserGroup>().eq(RbacUserGroup::getGroupId, id));
        evictAllPermissionCache();
    }

    @Override
    public List<Integer> userGroupIds(Integer userId) {
        List<RbacUserGroup> links = userGroupMapper.selectList(
                new LambdaQueryWrapper<RbacUserGroup>().eq(RbacUserGroup::getUserId, userId));
        List<Integer> ids = new ArrayList<>();
        for (RbacUserGroup link : links) {
            ids.add(link.getGroupId());
        }
        return ids;
    }

    @Override
    public void saveUserGroups(Integer userId, List<Integer> groupIds) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        userGroupMapper.delete(new LambdaQueryWrapper<RbacUserGroup>().eq(RbacUserGroup::getUserId, userId));
        if (groupIds != null) {
            Set<Integer> unique = new LinkedHashSet<>(groupIds);
            for (Integer groupId : unique) {
                if (groupId == null || groupMapper.selectById(groupId) == null) {
                    continue;
                }
                RbacUserGroup link = new RbacUserGroup();
                link.setUserId(userId);
                link.setGroupId(groupId);
                userGroupMapper.insert(link);
            }
        }
        redis.delete(PERM_KEY_PREFIX + userId);
    }

    @Override
    public void assignDefaultGroup(Integer userId, String userRole) {
        String groupName = "ADMIN".equals(userRole) ? "管理员组"
                : "TEACHER".equals(userRole) ? "教师组" : "学生组";
        RbacGroup group = groupMapper.selectOne(new LambdaQueryWrapper<RbacGroup>()
                .eq(RbacGroup::getName, groupName).last("limit 1"));
        if (group == null) {
            return;
        }
        Long exists = userGroupMapper.selectCount(new LambdaQueryWrapper<RbacUserGroup>()
                .eq(RbacUserGroup::getUserId, userId)
                .eq(RbacUserGroup::getGroupId, group.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        RbacUserGroup link = new RbacUserGroup();
        link.setUserId(userId);
        link.setGroupId(group.getId());
        userGroupMapper.insert(link);
        evictPermissionCache(userId);
    }

    @Override
    public void evictPermissionCache(Integer userId) {
        redis.delete(PERM_KEY_PREFIX + userId);
    }

    @Override
    public void evictAllPermissionCache() {
        Set<String> keys = redis.keys(PERM_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private RbacGroupVO toVO(RbacGroup group) {
        RbacGroupVO vo = new RbacGroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setDescription(group.getDescription());
        vo.setStatus(group.getStatus());
        List<RbacGroupPriv> privs = privMapper.selectList(new LambdaQueryWrapper<RbacGroupPriv>()
                .eq(RbacGroupPriv::getGroupId, group.getId()));
        List<String> keys = new ArrayList<>();
        for (RbacGroupPriv priv : privs) {
            keys.add(priv.getModule() + ":" + priv.getMethod());
        }
        vo.setPrivileges(keys);
        return vo;
    }
}
