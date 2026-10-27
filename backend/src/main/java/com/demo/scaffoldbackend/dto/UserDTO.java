package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 用户新增/更新请求体
 */
@Data
public class UserDTO {

    /** 登录账号 */
    private String userName;

    /** 加密密码 */
    private String pwd;

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
}
