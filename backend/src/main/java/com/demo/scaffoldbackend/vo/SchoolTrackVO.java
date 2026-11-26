package com.demo.scaffoldbackend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人今日院校行为明细响应体（浏览 / 关注）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolTrackVO {

    /** 院校名称 */
    private String schoolName;

    /** 动作类型：VIEW 浏览 / FOLLOW 关注 */
    private String action;
}
