package com.demo.scaffoldbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话成员：记录已读游标与未读数
 */
@Data
@TableName("chat_session_member")
public class ChatSessionMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Integer userId;

    /** OWNER / MEMBER */
    private String memberRole;

    /** 已读游标：最后一条已读消息ID */
    private Long lastReadMsgId;

    private Integer unreadCount;

    private LocalDateTime joinTime;
}
