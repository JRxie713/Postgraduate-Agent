package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.dto.PracticeAnswerDTO;
import com.demo.scaffoldbackend.vo.PracticeAnswerResultVO;
import com.demo.scaffoldbackend.vo.PracticeOptionsVO;
import com.demo.scaffoldbackend.vo.PracticeQuestionVO;
import com.demo.scaffoldbackend.vo.PracticeRecordVO;
import com.demo.scaffoldbackend.vo.PracticeSummaryVO;

import java.util.List;

/**
 * 刷题训练服务
 */
public interface PracticeService {

    /** 分页查询练习题（科目 / 章节 / 知识点 / 难度 / 关键词） */
    PageResult<PracticeQuestionVO> pageQuestions(Integer userId, int page, int size, String examSubject,
                                                 String chapter, String knowledgePoint, String difficulty,
                                                 String keyword);

    /** 查询练习题详情 */
    PracticeQuestionVO getQuestion(Integer userId, Integer questionId);

    /** 提交客观题答案并判题，答错自动收录错题 */
    PracticeAnswerResultVO submitAnswer(Integer userId, PracticeAnswerDTO dto);

    /** 分页查询日常练习记录 */
    PageResult<PracticeRecordVO> pageRecords(Integer userId, int page, int size, String examSubject,
                                             String knowledgePoint, Boolean onlyWrong);

    /** 练习概览统计 */
    PracticeSummaryVO summary(Integer userId);

    /** 同知识点同类题（专项练习） */
    List<PracticeQuestionVO> similarQuestions(Integer userId, Integer questionId, int limit);

    /** 每日一练：按题量随机抽题，可搭配科目 / 难度筛选 */
    List<PracticeQuestionVO> dailyQuestions(Integer userId, int count, String examSubject, String difficulty);

    /** 筛选项（科目 / 章节 / 知识点 / 难度） */
    PracticeOptionsVO options();
}
