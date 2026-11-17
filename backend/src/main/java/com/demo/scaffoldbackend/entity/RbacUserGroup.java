package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户与权限组的关联（对应禅道 zt_usergroup）
 */
@Data
@TableName("zt_usergroup")
public class RbacUserGroup {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer groupId;
}
