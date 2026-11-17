package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 权限组响应体：包含组信息 + 已勾选的权限点
 */
@Data
public class RbacGroupVO {

    private Integer id;

    private String name;

    private String description;

    private String status;

    /** 已拥有的权限点，如 user:view */
    private List<String> privileges;
}
