package com.demo.scaffoldbackend.rbac;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限点目录：与禅道的 module + method 一致，鉴权时用 "module:method" 字符串。
 * 新增受控接口时，在这里登记一个权限点即可。
 */
public final class PermissionCatalog {

    public record Item(String module, String moduleLabel, String method, String methodLabel) {
        public String key() {
            return module + ":" + method;
        }
    }

    public static final List<Item> ITEMS = List.of(
            new Item("user", "用户管理", "view", "查看用户"),
            new Item("user", "用户管理", "create", "新增用户"),
            new Item("user", "用户管理", "update", "编辑用户"),
            new Item("user", "用户管理", "delete", "删除用户"),
            new Item("user", "用户管理", "unlock", "解除登录锁"),
            new Item("policy", "政策资讯", "view", "查看资讯"),
            new Item("policy", "政策资讯", "create", "发布资讯"),
            new Item("policy", "政策资讯", "update", "修改资讯"),
            new Item("policy", "政策资讯", "delete", "删除资讯"),
            new Item("rbac", "权限管理", "view", "查看权限组"),
            new Item("rbac", "权限管理", "manage", "管理权限组")
    );

    private PermissionCatalog() {
    }

    public static Set<String> allKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Item item : ITEMS) {
            keys.add(item.key());
        }
        return keys;
    }

    public static boolean exists(String key) {
        return allKeys().contains(key);
    }
}
