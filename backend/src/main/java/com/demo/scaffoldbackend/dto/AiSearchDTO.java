package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 全局语义检索请求体
 */
@Data
public class AiSearchDTO {

    /** 用户ID */
    private Integer userId;

    /** 检索关键词 */
    private String keyword;

    /** 检索范围 */
    private List<String> scopes;

    /** 考试科目 */
    private String examSubject;

    /** 页码 */
    private Integer page;

    /** 每页大小 */
    private Integer size;
}
