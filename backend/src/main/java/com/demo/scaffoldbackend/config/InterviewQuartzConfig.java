package com.demo.scaffoldbackend.config;

import com.demo.scaffoldbackend.task.InterviewIdleReminderJob;
import com.demo.scaffoldbackend.task.InterviewReminderJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 复试模块 Quartz 定时任务配置
 *
 * <ul>
 *   <li>每日 09:00：扫描超过 1 天未查看的面试报告</li>
 *   <li>每 5 分钟：扫描进行中但超过 5 分钟未作答的面试，便于演示</li>
 * </ul>
 *
 * 两条 cron 均可通过配置覆盖：interview.reminder.cron / interview.reminder.idle-cron
 */
@Configuration
public class InterviewQuartzConfig {

    /** 默认：每天 09:00 */
    @Value("${interview.reminder.cron:0 0 9 * * ?}")
    private String cron;

    /** 默认：每 5 分钟 */
    @Value("${interview.reminder.idle-cron:0 */5 * * * ?}")
    private String idleCron;

    @Bean
    public JobDetail interviewReminderJobDetail() {
        return JobBuilder.newJob(InterviewReminderJob.class)
                .withIdentity("interviewReminderJob", "interview")
                .withDescription("面试报告未读提醒")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger interviewReminderTrigger(JobDetail interviewReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(interviewReminderJobDetail)
                .withIdentity("interviewReminderTrigger", "interview")
                .withDescription("每天 09:00 提醒未读面试报告")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public JobDetail interviewIdleReminderJobDetail() {
        return JobBuilder.newJob(InterviewIdleReminderJob.class)
                .withIdentity("interviewIdleReminderJob", "interview")
                .withDescription("面试超时未作答提醒")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger interviewIdleReminderTrigger(JobDetail interviewIdleReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(interviewIdleReminderJobDetail)
                .withIdentity("interviewIdleReminderTrigger", "interview")
                .withDescription("每 5 分钟提醒未作答的面试")
                .withSchedule(CronScheduleBuilder.cronSchedule(idleCron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
