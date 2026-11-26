package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成追问请求体
 */
@Data
public class FollowUpDTO {

    /** 会话ID */
    @NotNull(message = "会话ID不能为空")
    private Integer sessionId;

    /** 当前第几轮(基于这一轮回答生成追问) */
    @NotNull(message = "轮次不能为空")
    private Integer roundIndex;
}
