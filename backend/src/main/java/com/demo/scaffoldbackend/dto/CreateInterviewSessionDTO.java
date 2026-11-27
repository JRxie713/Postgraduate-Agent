package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建面试会话请求体（开始模拟面试）
 */
@Data
public class CreateInterviewSessionDTO {

    /** 用户ID */
    private Integer userId;

    /** 训练目标专业, 不能为空 */
    @NotBlank(message = "专业不能为空")
    @Size(max = 50)
    private String major;

    /** 训练类型: comprehensive/professional/pressure/english */
    @NotBlank(message = "训练类型不能为空")
    private String trainingType;

    /** 难度: easy/medium/hard, 默认 medium */
    private String difficulty = "medium";

    /** 是否开启压力面试 */
    private Boolean pressureMode = false;

    /** 报考方向 / 简历要点（可选）：配置 AI Key 时用于生成更贴合个人的题目 */
    @Size(max = 500)
    private String background;
}
