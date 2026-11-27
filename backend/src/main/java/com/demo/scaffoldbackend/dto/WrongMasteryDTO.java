package com.demo.scaffoldbackend.dto;

import lombok.Data;

/**
 * 错题掌握状态更新请求体
 */
@Data
public class WrongMasteryDTO {

    /** 掌握程度：MASTERED 已掌握 / UNMASTERED 仍不会 / REVIEWING 复习中 */
    private String masteryLevel;
}
