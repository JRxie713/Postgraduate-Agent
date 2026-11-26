package com.demo.scaffoldbackend.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.scaffoldbackend.entity.InterviewReport;
import com.demo.scaffoldbackend.entity.InterviewSession;
import com.demo.scaffoldbackend.mapper.InterviewReportMapper;
import com.demo.scaffoldbackend.mapper.InterviewSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 面试进行中提醒（每 5 分钟）
 *
 * <p>扫描"进行中但超过 5 分钟没有新作答"的面试会话，提醒用户回到训练；
 * 同时输出未读报告数量。用于课堂演示定时任务持续触发。</p>
 */
@Slf4j
@DisallowConcurrentExecution
public class InterviewIdleReminderJob extends QuartzJobBean {

    @Autowired
    private InterviewSessionMapper sessionMapper;

    @Autowired
    private InterviewReportMapper reportMapper;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<InterviewSession> idle = sessionMapper.selectList(Wrappers.<InterviewSession>lambdaQuery()
                .eq(InterviewSession::getStatus, "ongoing")
                .lt(InterviewSession::getUpdateTime, threshold));

        long unreadReports = reportMapper.selectCount(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getReportRead, 0));

        if (idle.isEmpty()) {
            log.info("[面试提醒·5分钟] 无超时未作答会话，未读报告 {} 份", unreadReports);
            return;
        }

        String detail = idle.stream()
                .map(s -> String.format("sessionId=%d(userId=%d, 已答%d/%d轮)",
                        s.getId(), s.getUserId(), s.getCurrentRound(), s.getTotalRounds()))
                .collect(Collectors.joining("; "));
        log.info("[面试提醒·5分钟] {} 场面试超过 5 分钟未作答：{}；未读报告 {} 份",
                idle.size(), detail, unreadReports);
    }
}
