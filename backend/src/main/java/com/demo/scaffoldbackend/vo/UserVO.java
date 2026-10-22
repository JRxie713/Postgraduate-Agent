package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应体
 */
@Data
public class UserVO {

    /** 主键 */
    private Integer id;

    /** 登录账号 */
    private String userName;

    /** 用户姓名 */
    private String realName;

    /** 联系电话 */
    private String phone;

    /** 角色：STUDENT/TEACHER/ADMIN */
    private String userRole;

    /** 学生学号 */
    private String studentNo;

    /** 意向报考专业 */
    private String targetMajor;

    /** 督导模式 */
    private String superviseMode;

    /** 账号状态 */
    private String userStatus;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录修改时间 */
    private LocalDateTime updateTime;
}
