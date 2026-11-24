package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话：私聊（PRIVATE）或群聊/自习室（GROUP）
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** PRIVATE / GROUP */
    private String type;

    /** 群聊名称 */
    private String name;

    /** 私聊唯一键：minUserId:maxUserId */
    private String pairKey;

    /** 群主用户ID */
    private Integer ownerId;

    private Long lastMsgId;

    private String lastMsgPreview;

    private LocalDateTime lastMsgTime;

    private Integer memberCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
