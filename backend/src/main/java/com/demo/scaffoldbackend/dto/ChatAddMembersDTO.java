package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 群聊添加成员
 */
@Data
public class ChatAddMembersDTO {

    private List<Integer> memberIds;
}
