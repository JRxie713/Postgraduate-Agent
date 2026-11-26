package com.demo.scaffoldbackend.config;

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
 * <p>默认每天 09:00 扫描未读面试报告，可通过 interview.reminder.cron 覆盖。</p>
 */
@Configuration
public class InterviewQuartzConfig {

    /** 默认：每天 09:00 */
    @Value("${interview.reminder.cron:0 0 9 * * ?}")
    private String cron;

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
}
