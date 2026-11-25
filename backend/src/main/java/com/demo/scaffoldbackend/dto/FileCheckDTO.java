package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 分片上传：上传前检查（check 接口）的请求参数
 */
@Data
public class FileCheckDTO {

    /** 文件唯一标识，由前端生成；同一个文件刷新页面后必须还是同一个值，否则续传就失效了 */
    private String fileId;

    /** 原始文件名 */
    private String fileName;

    /** 这个文件一共要切成多少片 */
    private Integer totalChunks;
}