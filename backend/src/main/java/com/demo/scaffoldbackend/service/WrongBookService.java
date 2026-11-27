package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.vo.WrongBookQuestionVO;

/**
 * 错题本服务
 */
public interface WrongBookService {

    /** 分页查询错题（科目 / 知识点 / 掌握状态） */
    PageResult<WrongBookQuestionVO> pageWrongQuestions(Integer userId, int page, int size,
                                                       String examSubject, String knowledgePoint,
                                                       String masteryLevel);

    /** 查询错题详情 */
    WrongBookQuestionVO getWrongQuestion(Integer userId, Integer id);

    /** 标记已掌握 / 仍不会 */
    WrongBookQuestionVO updateMastery(Integer userId, Integer id, String masteryLevel);

    /** 删除错题 */
    boolean deleteWrongQuestion(Integer userId, Integer id);
}
