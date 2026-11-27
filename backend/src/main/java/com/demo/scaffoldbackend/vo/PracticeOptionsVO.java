package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 练习题筛选项
 */
@Data
public class PracticeOptionsVO {

    /** 全部科目 */
    private List<String> subjects;

    /** 全部难度 */
    private List<String> difficulties;

    /** 科目 -> 章节列表 */
    private Map<String, List<String>> chapters;

    /** 科目 -> 知识点列表 */
    private Map<String, List<String>> knowledgePoints;
}
