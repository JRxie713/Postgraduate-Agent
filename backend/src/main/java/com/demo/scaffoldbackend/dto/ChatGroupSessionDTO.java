package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建群聊/自习室
 */
@Data
public class ChatGroupSessionDTO {

    private String name;

    /** 初始成员（不含创建者） */
    private List<Integer> memberIds;
}
