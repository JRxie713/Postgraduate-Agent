package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 组权限：一个权限组拥有哪些 module:method 权限点（对应禅道 zt_grouppriv）
 */
@Data
@TableName("zt_grouppriv")
public class RbacGroupPriv {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer groupId;

    /** 模块，如 user */
    private String module;

    /** 方法，如 view */
    private String method;
}
