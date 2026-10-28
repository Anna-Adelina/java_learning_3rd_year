package com.booktracker.booktracker.job;

import com.booktracker.booktracker.service.ReadingReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ReadingReminderJob implements Job {

    public static final String KEY_REMIND_EVERY_DAYS = "remindEveryDays";
    public static final String KEY_BATCH_LIMIT = "batchLimit";
    static final int MAX_REFIRES = 3;

    private final ReadingReminderService service;

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data = ctx.getMergedJobDataMap();
        int days;
        int limit;
        try {
            days = JobParams.positiveInt(data, KEY_REMIND_EVERY_DAYS);
            limit = JobParams.positiveInt(data, KEY_BATCH_LIMIT);
        } catch (JobParams.JobParameterException e) {
            throw new JobExecutionException(e, false);   // помилка конфігурації, повтор безглуздий
        }
        try {
            int sent = service.sendReminders(days, limit);
            log.info("Reading reminders sent: {}", sent);
        } catch (RuntimeException e) {
            if (ctx.getRefireCount() < MAX_REFIRES) {
                log.warn("Reminder job failed, refire #{}", ctx.getRefireCount() + 1, e);
                throw new JobExecutionException(e, true);   // повторити одразу
            }
            log.error("Reminder job gave up after {} refires; waiting for next cron", MAX_REFIRES, e);
        }
    }
}