package com.demo.scaffoldbackend.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 主观题 AI 批改响应体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiGradeVO extends UserAnswerRecordVO {

    /** 得分点明细 */
    private List<GradePoint> gradePoints;

    /** 评语 */
    private String comment;

    /** 改写建议 */
    private String rewriteSuggestion;

    /**
     * 得分点
     */
    @Data
    public static class GradePoint {

        /** 得分点 */
        private String point;

        /** 是否命中 */
        private Boolean hit;

        /** 该点得分 */
        private Double score;
    }
}
