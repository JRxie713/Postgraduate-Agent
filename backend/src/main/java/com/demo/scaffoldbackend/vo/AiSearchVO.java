package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语义检索 / 个性化政策信息流响应体
 */
@Data
public class AiSearchVO {

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页大小 */
    private Integer size;

    /** 数据列表 */
    private List<Record> records;

    /**
     * 检索 / 政策记录
     */
    @Data
    public static class Record {

        /** 结果类型：QUESTION / KNOWLEDGE / POLICY */
        private String type;

        /** 资源ID */
        private Integer id;

        /** 标题 */
        private String title;

        /** 摘要片段 */
        private String snippet;

        /** 相关度 */
        private Double score;

        /** 通知类型 */
        private String noticeType;

        /** 信息来源 */
        private String noticeSource;

        /** 生效时间 */
        private LocalDateTime effectTime;

        /** 标签 */
        private String subTags;

        /** 匹配度 */
        private Double matchScore;
    }
}
