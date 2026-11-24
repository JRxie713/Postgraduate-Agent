package com.demo.scaffoldbackend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.dto.LearningLogDTO;
import com.demo.scaffoldbackend.entity.LearningLog;
import com.demo.scaffoldbackend.vo.LearningLogVO;

import java.util.List;

public interface LearningLogService extends IService<LearningLog> {

    List<LearningLogVO> listByUserId(Integer userId);

    PageResult<LearningLogVO> pageLogs(Integer page, Integer size);

    LearningLogVO getLogById(Integer id);

    LearningLogVO checkIn(LearningLogDTO logDTO);

    LearningLogVO updateLog(Integer id, LearningLogDTO logDTO);

    boolean deleteLog(Integer id);
}
