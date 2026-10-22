package com.demo.scaffoldbackend.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页返回结构
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页大小 */
    private int size;

    /** 数据列表 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, int page, int size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, int page, int size, List<T> records) {
        return new PageResult<>(total, page, size, records);
    }
}
