package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学习打卡请求体
 */
@Data
public class StudyCheckinDTO {

    /** 用户 ID */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /** 本次学习时长（分钟），1 ~ 1440 */
    @Min(value = 1, message = "学习时长至少 1 分钟")
    @Max(value = 1440, message = "学习时长不能超过一天（1440 分钟）")
    private Integer minutes;

    /** 学习科目（可选）：数学 / 英语 / 政治 / 专业课 */
    private String subject;
}
