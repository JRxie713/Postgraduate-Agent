package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限点目录（按模块分组），供权限组管理页面渲染勾选框
 */
@Data
public class PermissionModuleVO {

    private String module;

    private String moduleLabel;

    private List<Method> methods = new ArrayList<>();

    @Data
    public static class Method {
        private String method;
        private String label;

        public Method(String method, String label) {
            this.method = method;
            this.label = label;
        }
    }
}
