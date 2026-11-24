package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天附件
 */
@Data
@TableName("chat_file")
public class ChatFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Integer uploaderId;

    private String fileName;

    private Long fileSize;

    private String contentType;

    /** 对象存储 key（OSS 或本机相对路径） */
    private String storageKey;

    private LocalDateTime createTime;
}
