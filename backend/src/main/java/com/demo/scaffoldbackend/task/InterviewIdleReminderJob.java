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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 面试进行中提醒（默认每 5 分钟）
 *
 * <p>扫描"进行中但超过 N 分钟没有新作答"的面试会话，提醒用户回到训练；
 * 同时输出未读报告数量。</p>
 *
 * <p><b>N 从哪里来：</b>由 {@code interview.reminder.idle-minutes} 决定，
 * 本类第 38 行的 {@code :5} 只是"没配置时的默认值"。
 * 只要 backend/.env 里写了这行，就以 .env 为准（配置优先于代码默认值），
 * 这样改分钟数不用重新编译；同一数字也被 InterviewQuartzConfig 用来生成 cron，
 * 保证"多久跑一次"和"多久算超时"始终一致。</p>
 */
@Slf4j
@DisallowConcurrentExecution
public class InterviewIdleReminderJob extends QuartzJobBean {

    @Autowired
    private InterviewSessionMapper sessionMapper;

    @Autowired
    private InterviewReportMapper reportMapper;

    /** 空闲判定阈值（分钟），默认 5；backend/.env 配了则以 .env 为准 */
    @Value("${interview.reminder.idle-minutes:5}")
    private int idleMinutes;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(idleMinutes);

        List<InterviewSession> idle = sessionMapper.selectList(Wrappers.<InterviewSession>lambdaQuery()
                .eq(InterviewSession::getStatus, "ongoing")
                .lt(InterviewSession::getUpdateTime, threshold));

        long unreadReports = reportMapper.selectCount(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getReportRead, 0));

        if (idle.isEmpty()) {
            log.info("[面试提醒·{}分钟] 无超时未作答会话，未读报告 {} 份", idleMinutes, unreadReports);
            return;
        }

        String detail = idle.stream()
                .map(s -> String.format("sessionId=%d(userId=%d, 已答%d/%d轮)",
                        s.getId(), s.getUserId(), s.getCurrentRound(), s.getTotalRounds()))
                .collect(Collectors.joining("; "));
        log.info("[面试提醒·{}分钟] {} 场面试超过 {} 分钟未作答：{}；未读报告 {} 份",
                idleMinutes, idle.size(), idleMinutes, detail, unreadReports);
    }
}
