package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表/详情
 */
@Data
public class ChatSessionVO {

    private Long id;

    /** PRIVATE / GROUP */
    private String type;

    /** 群聊名称；私聊显示对方姓名 */
    private String name;

    private Integer peerId;

    private String peerName;

    private String peerRole;

    private Integer ownerId;

    private Long lastMsgId;

    private String lastMsgPreview;

    private LocalDateTime lastMsgTime;

    private Integer memberCount;

    private Integer unreadCount;

    private Long lastReadMsgId;
}
