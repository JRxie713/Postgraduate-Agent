package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户的权限视图，供前端控制菜单与按钮
 */
@Data
public class MyPermissionsVO {

    private Integer userId;

    private String userRole;

    private List<String> permissions;
}
