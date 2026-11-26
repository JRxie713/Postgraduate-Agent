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
 * 面试报告未读提醒定时任务
 *
 * <p>每天扫描"已生成报告但用户超过 1 天未查看"的面试会话，输出提醒日志。
 * 后续可在此处接入站内信 / 邮件通知。</p>
 */
@Slf4j
@DisallowConcurrentExecution
public class InterviewReminderJob extends QuartzJobBean {

    @Autowired
    private InterviewReportMapper reportMapper;

    @Autowired
    private InterviewSessionMapper sessionMapper;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<InterviewReport> unread = reportMapper.selectList(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getReportRead, 0)
                .lt(InterviewReport::getCreateTime, threshold));

        if (unread.isEmpty()) {
            log.info("[面试提醒] 无未读面试报告，任务结束");
            return;
        }

        String detail = unread.stream()
                .map(r -> {
                    InterviewSession session = sessionMapper.selectById(r.getSessionId());
                    String major = session == null ? "-" : session.getMajor();
                    Integer userId = session == null ? null : session.getUserId();
                    return String.format("sessionId=%d, userId=%s, major=%s, total=%s",
                            r.getSessionId(), userId, major, r.getTotalScore());
                })
                .collect(Collectors.joining("; "));

        log.info("[面试提醒] 发现 {} 份超过 1 天未查看的面试报告：{}", unread.size(), detail);
    }
}
