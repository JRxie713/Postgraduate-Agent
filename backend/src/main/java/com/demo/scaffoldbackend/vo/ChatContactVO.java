package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 可发起会话的联系人
 */
@Data
public class ChatContactVO {

    private Integer id;

    private String name;

    private String userRole;
}
