package com.booktracker.booktracker.config;

import com.booktracker.booktracker.job.ReadingReminderJob;
import com.booktracker.booktracker.job.ReadingReportJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    public static final String REMINDERS_GROUP = "reminders";
    public static final String REPORTS_GROUP = "reports";
    public static final JobKey REMINDER_JOB_KEY = new JobKey("readingReminderJob", REMINDERS_GROUP);
    public static final JobKey REPORT_JOB_KEY = new JobKey("readingReportJob", REPORTS_GROUP);
    public static final TimeZone SCHEDULE_ZONE = TimeZone.getTimeZone("Europe/Kyiv");

    // ---------- Reminder: 1 job, 1 trigger ----------

    @Bean
    public JobDetail readingReminderJobDetail() {
        return JobBuilder.newJob(ReadingReminderJob.class)
                .withIdentity(REMINDER_JOB_KEY)
                .usingJobData(ReadingReminderJob.KEY_REMIND_EVERY_DAYS, 7)
                .usingJobData(ReadingReminderJob.KEY_BATCH_LIMIT, 100)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger readingReminderDailyTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REMINDER_JOB_KEY)
                .withIdentity("readingReminderDaily", REMINDERS_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                        .inTimeZone(SCHEDULE_ZONE)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    // ---------- Report: 1 job, 2 triggers ----------

    @Bean
    public JobDetail readingReportJobDetail() {
        return JobBuilder.newJob(ReadingReportJob.class)
                .withIdentity(REPORT_JOB_KEY)
                .usingJobData(ReadingReportJob.KEY_PERIOD_TYPE, "WEEKLY")
                .usingJobData(ReadingReportJob.KEY_MIN_BOOKS, 1)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger readingReportWeeklyTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REPORT_JOB_KEY)
                .withIdentity("readingReportWeekly", REPORTS_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 ? * MON")
                        .inTimeZone(SCHEDULE_ZONE)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public Trigger readingReportMonthlyTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REPORT_JOB_KEY)
                .withIdentity("readingReportMonthly", REPORTS_GROUP)
                .usingJobData(ReadingReportJob.KEY_PERIOD_TYPE, "MONTHLY")   // перевизначає лише цей ключ
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 1 * ?")
                        .inTimeZone(SCHEDULE_ZONE)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}