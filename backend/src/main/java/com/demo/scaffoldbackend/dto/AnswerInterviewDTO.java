package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交回答请求体
 */
@Data
public class AnswerInterviewDTO {

    /** 会话ID */
    @NotNull(message = "会话ID不能为空")
    private Integer sessionId;

    /** 当前第几轮(从1开始) */
    @NotNull(message = "轮次不能为空")
    private Integer roundIndex;

    /** 学生回答文本 */
    private String answer;
}
