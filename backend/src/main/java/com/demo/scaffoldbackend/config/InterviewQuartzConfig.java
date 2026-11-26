package com.demo.scaffoldbackend.config;

import com.demo.scaffoldbackend.task.InterviewIdleReminderJob;
import com.demo.scaffoldbackend.task.InterviewReminderJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * 复试模块 Quartz 定时任务配置
 *
 * <ul>
 *   <li>每日 09:00：扫描超过 1 天未查看的面试报告</li>
 *   <li>每 5 分钟：扫描进行中但超过 N 分钟未作答的面试，便于演示</li>
 * </ul>
 *
 * 触发周期可通过配置覆盖，例如演示时想 1 分钟一次：
 * <pre>
 * interview.reminder.idle-cron = 0 0/1 * * * ?
 * interview.reminder.idle-minutes = 1
 * </pre>
 */
@Slf4j
@Configuration
public class InterviewQuartzConfig {

    /** 默认：每天 09:00 */
    @Value("${interview.reminder.cron:0 0 9 * * ?}")
    private String cron;

    /** 默认：每 5 分钟 */
    @Value("${interview.reminder.idle-cron:0 */5 * * * ?}")
    private String idleCron;

    @Autowired(required = false)
    private Scheduler scheduler;

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
                .withDescription("定时提醒未作答的面试")
                .withSchedule(CronScheduleBuilder.cronSchedule(idleCron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * 启动完成后打印两个任务的下次触发时间，方便确认定时任务确实生效
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logScheduledTriggers() {
        if (scheduler == null) {
            log.warn("[Quartz] 调度器未就绪，无法输出定时任务信息");
            return;
        }
        logNextFire("interviewReminderTrigger", "每日未读报告提醒");
        logNextFire("interviewIdleReminderTrigger", "超时未作答提醒");
    }

    private void logNextFire(String triggerName, String desc) {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(triggerName, "interview"));
            if (trigger == null) {
                log.warn("[Quartz] 未找到触发器 {}", triggerName);
                return;
            }
            String expression = trigger instanceof CronTrigger c ? c.getCronExpression() : "-";
            log.info("[Quartz] {} 已注册，cron=[{}]，下次触发时间={}",
                    desc, expression, trigger.getNextFireTime());
        } catch (Exception e) {
            log.warn("[Quartz] 读取触发器 {} 失败: {}", triggerName, e.getMessage());
        }
    }
}
