package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.dto.LearningLogDTO;
import com.demo.scaffoldbackend.entity.LearningLog;
import com.demo.scaffoldbackend.mapper.LearningLogMapper;
import com.demo.scaffoldbackend.service.LearningLogService;
import com.demo.scaffoldbackend.vo.LearningLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearningLogServiceImpl extends ServiceImpl<LearningLogMapper, LearningLog>
        implements LearningLogService {

    @Override
    public List<LearningLogVO> listByUserId(Integer userId) {
        List<LearningLog> logs = lambdaQuery()
                .eq(LearningLog::getUserId, userId)
                .orderByDesc(LearningLog::getLogDate)
                .list();
        return toVOList(logs);
    }

    @Override
    public PageResult<LearningLogVO> pageLogs(Integer page, Integer size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("页码必须大于0，每页数量必须在1到100之间");
        }
        Page<LearningLog> logPage = page(Page.of(page, size),
                Wrappers.<LearningLog>lambdaQuery().orderByDesc(LearningLog::getLogDate));
        return PageResult.of(logPage.getTotal(), page, size, toVOList(logPage.getRecords()));
    }

    @Override
    public LearningLogVO getLogById(Integer id) {
        return toVO(requireLog(id));
    }

    @Override
    @Transactional
    public LearningLogVO checkIn(LearningLogDTO logDTO) {
        if (logDTO.getUserId() == null || logDTO.getLogDate() == null) {
            throw new IllegalArgumentException("用户ID和记录日期不能为空");
        }

        LearningLog learningLog = new LearningLog();
        BeanUtils.copyProperties(logDTO, learningLog);
        learningLog.setLogDate(normalizeLogDate(logDTO.getLogDate()));
        learningLog.setCheckIn(true);

        LearningLog existing = lambdaQuery()
                .eq(LearningLog::getUserId, learningLog.getUserId())
                .eq(LearningLog::getLogDate, learningLog.getLogDate())
                .one();
        if (existing == null) {
            save(learningLog);
        } else {
            learningLog.setId(existing.getId());
            updateById(learningLog);
        }
        return toVO(requireLog(learningLog.getId()));
    }

    @Override
    @Transactional
    public LearningLogVO updateLog(Integer id, LearningLogDTO logDTO) {
        LearningLog learningLog = requireLog(id);
        BeanUtils.copyProperties(logDTO, learningLog);
        if (logDTO.getLogDate() != null) {
            learningLog.setLogDate(normalizeLogDate(logDTO.getLogDate()));
        }
        learningLog.setCreateTime(null);
        learningLog.setUpdateTime(null);
        updateById(learningLog);
        return toVO(requireLog(id));
    }

    @Override
    public boolean deleteLog(Integer id) {
        return removeById(id);
    }

    private LearningLog requireLog(Integer id) {
        LearningLog learningLog = getById(id);
        if (learningLog == null) {
            throw new IllegalArgumentException("学习日志不存在");
        }
        return learningLog;
    }

    private LocalDateTime normalizeLogDate(LocalDateTime logDate) {
        return logDate.toLocalDate().atStartOfDay();
    }

    private List<LearningLogVO> toVOList(List<LearningLog> logs) {
        return logs.stream().map(this::toVO).toList();
    }

    private LearningLogVO toVO(LearningLog learningLog) {
        LearningLogVO vo = new LearningLogVO();
        BeanUtils.copyProperties(learningLog, vo);
        return vo;
    }
}
