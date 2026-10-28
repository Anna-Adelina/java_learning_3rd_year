package com.booktracker.booktracker.config;

import com.booktracker.booktracker.job.ReadingReportJob;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.Trigger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzConfigTest {

    private final QuartzConfig config = new QuartzConfig();

    private List<Trigger> allTriggers() {
        return List.of(
                config.readingReminderDailyTrigger(),
                config.readingReportWeeklyTrigger(),
                config.readingReportMonthlyTrigger());
    }

    @Test
    void triggersPointToJobsByKey_andHaveNameAndGroup() {
        assertEquals(QuartzConfig.REMINDER_JOB_KEY, config.readingReminderDailyTrigger().getJobKey());
        assertEquals(QuartzConfig.REPORT_JOB_KEY, config.readingReportWeeklyTrigger().getJobKey());
        assertEquals(QuartzConfig.REPORT_JOB_KEY, config.readingReportMonthlyTrigger().getJobKey());

        allTriggers().forEach(t -> {
            assertFalse(t.getKey().getName().isBlank());
            assertFalse(t.getKey().getGroup().isBlank());
        });
        assertEquals(QuartzConfig.REMINDER_JOB_KEY, config.readingReminderJobDetail().getKey());
        assertEquals(QuartzConfig.REPORT_JOB_KEY, config.readingReportJobDetail().getKey());
    }

    @Test
    void triggerDataOverridesJobDefaults_andUnsetKeysKeepDefaults() {
        JobDataMap merged = new JobDataMap();   // так Quartz будує merged map: спочатку job, поверх trigger
        merged.putAll(config.readingReportJobDetail().getJobDataMap());
        merged.putAll(config.readingReportMonthlyTrigger().getJobDataMap());

        assertEquals("MONTHLY", merged.getString(ReadingReportJob.KEY_PERIOD_TYPE));  // з тригера
        assertEquals(1, merged.getInt(ReadingReportJob.KEY_MIN_BOOKS));               // дефолт з job
    }

    @Test
    void jobsDisallowConcurrentExecution() {
        assertTrue(config.readingReminderJobDetail().isConcurrentExecutionDisallowed());
        assertTrue(config.readingReportJobDetail().isConcurrentExecutionDisallowed());
    }

    @Test
    void misfireInstructionsAreExplicit() {
        CronTrigger reminder = (CronTrigger) config.readingReminderDailyTrigger();
        CronTrigger weekly = (CronTrigger) config.readingReportWeeklyTrigger();
        CronTrigger monthly = (CronTrigger) config.readingReportMonthlyTrigger();

        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, reminder.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, weekly.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, monthly.getMisfireInstruction());
    }

    @Test
    void cronTriggersHaveExplicitTimeZone() {
        allTriggers().forEach(t ->
                assertEquals("Europe/Kyiv", ((CronTrigger) t).getTimeZone().getID()));
    }
}