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
 * <p>每日任务：09:00 扫描超过 1 天未查看的面试报告。</p>
 *
 * <p>超时提醒任务：只需要配置一个数字 {@code interview.reminder.idle-minutes}，
 * 它同时决定"多久触发一次"和"多久没作答算超时"，两者始终一致。
 * 例如填 1 表示每分钟检查一次、超过 1 分钟未作答就提醒；填 3 则每 3 分钟检查一次。</p>
 */
@Slf4j
@Configuration
public class InterviewQuartzConfig {

    /** 每日提醒时间：默认每天 09:00 */
    @Value("${interview.reminder.cron:0 0 9 * * ?}")
    private String dailyCron;

    /** 超时提醒：N 分钟（同时作为触发周期与空闲阈值），默认 5 */
    @Value("${interview.reminder.idle-minutes:5}")
    private int idleMinutes;

    @Autowired(required = false)
    private Scheduler scheduler;

    /**
     * 由分钟数生成 cron：每 N 分钟的第 0 秒触发
     */
    private String idleCronExpression() {
        int minutes = Math.max(1, Math.min(59, idleMinutes));
        return "0 0/" + minutes + " * * * ?";
    }

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
                .withSchedule(CronScheduleBuilder.cronSchedule(dailyCron)
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
                .withDescription("按配置分钟数定时提醒未作答的面试")
                .withSchedule(CronScheduleBuilder.cronSchedule(idleCronExpression())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * 启动完成后打印两个任务的下次触发时间，方便确认定时任务已生效
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logScheduledTriggers() {
        if (scheduler == null) {
            log.warn("[Quartz] 调度器未就绪，无法输出定时任务信息");
            return;
        }
        log.info("[Quartz] 超时提醒配置：每 {} 分钟检查一次，超过 {} 分钟未作答即提醒", idleMinutes, idleMinutes);
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
