package com.demo.scaffoldbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 院校热度埋点请求体
 *
 * <p>学生在择校页查看 / 关注某院校时上报，后端先在 Redis 做当日去重与实时计数，
 * 再把事件投递到 Kafka 异步聚合历史热度。</p>
 */
@Data
public class SchoolHeatDTO {

    /** 操作用户 ID */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /** 院校专业记录 ID（用于链路追踪，热度排行按院校名称聚合） */
    @NotNull(message = "院校ID不能为空")
    private Integer schoolId;

    /** 院校名称 */
    @NotBlank(message = "院校名称不能为空")
    private String schoolName;

    /** 动作类型：VIEW=浏览，FOLLOW=关注，UNFOLLOW=取关；不传默认 VIEW */
    private String action;
}
