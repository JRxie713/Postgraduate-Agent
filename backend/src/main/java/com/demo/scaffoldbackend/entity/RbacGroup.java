package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限组（对应禅道 zt_group）
 */
@Data
@TableName("zt_group")
public class RbacGroup {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 组名称 */
    private String name;

    /** 说明 */
    private String description;

    /** NORMAL / DISABLED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
