package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 会话成员
 */
@Data
public class ChatMemberVO {

    private Integer userId;

    private String name;

    private String userRole;

    /** OWNER / MEMBER */
    private String memberRole;
}
