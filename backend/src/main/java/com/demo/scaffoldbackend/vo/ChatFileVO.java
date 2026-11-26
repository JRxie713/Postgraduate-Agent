package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天附件元数据
 */
@Data
public class ChatFileVO {

    private Long id;

    private Long sessionId;

    private String fileName;

    private Long fileSize;

    private String contentType;

    private LocalDateTime createTime;
}
