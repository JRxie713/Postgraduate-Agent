package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 权限组新增/更新请求体：名称、说明、状态以及勾选的权限点
 */
@Data
public class RbacGroupDTO {

    private String name;

    private String description;

    /** NORMAL / DISABLED，不传默认 NORMAL */
    private String status;

    /** 权限点列表，元素形如 user:view */
    private List<String> privileges;
}
