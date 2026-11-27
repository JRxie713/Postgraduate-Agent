package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自我介绍点评请求体（中英文均可）
 */
@Data
public class EnglishReviewDTO {

    /** 自我介绍文本 */
    @NotBlank(message = "自我介绍不能为空")
    @Size(max = 2000, message = "自我介绍最多2000字符")
    private String introduction;

    /** 用户ID(可选) */
    private Integer userId;
}
