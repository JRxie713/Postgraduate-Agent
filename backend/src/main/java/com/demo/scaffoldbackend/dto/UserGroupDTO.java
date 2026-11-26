package com.demo.scaffoldbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 给用户分配权限组
 */
@Data
public class UserGroupDTO {

    private List<Integer> groupIds;
}
