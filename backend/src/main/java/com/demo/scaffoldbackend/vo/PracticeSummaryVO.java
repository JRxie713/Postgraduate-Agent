package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 练习概览统计
 */
@Data
public class PracticeSummaryVO {

    /** 累计练习次数 */
    private Integer total;

    /** 答对次数 */
    private Integer correctCount;

    /** 正确率（0-100，保留一位小数） */
    private Double accuracy;

    /** 今日练习次数 */
    private Integer todayCount;

    /** 错题总数 */
    private Integer wrongTotal;

    /** 已掌握错题数 */
    private Integer masteredCount;
}
