package com.booktracker.booktracker.job;

import com.booktracker.booktracker.service.ReadingReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingReminderJobTest {

    @Mock
    ReadingReminderService service;

    @Mock
    JobExecutionContext ctx;

    @InjectMocks
    ReadingReminderJob job;

    private JobDataMap validData() {
        JobDataMap map = new JobDataMap();
        map.put(ReadingReminderJob.KEY_REMIND_EVERY_DAYS, 7);
        map.put(ReadingReminderJob.KEY_BATCH_LIMIT, 100);
        return map;
    }

    @Test
    void readsParamsAndCallsServiceExactlyOnce() throws Exception {
        when(ctx.getMergedJobDataMap()).thenReturn(validData());

        job.execute(ctx);

        verify(service, times(1)).sendReminders(7, 100);
    }

    @Test
    void missingKey_throwsExplicitError_withoutRefire() {
        JobDataMap map = validData();
        map.remove(ReadingReminderJob.KEY_BATCH_LIMIT);
        when(ctx.getMergedJobDataMap()).thenReturn(map);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(ctx));

        assertFalse(ex.refireImmediately());
        assertInstanceOf(JobParams.JobParameterException.class, ex.getCause());
    }

    @Test
    void misspelledKey_throwsExplicitError() {
        JobDataMap map = new JobDataMap();
        map.put("remindEveryDay", 7);   // помилка в назві ключа
        map.put(ReadingReminderJob.KEY_BATCH_LIMIT, 100);
        when(ctx.getMergedJobDataMap()).thenReturn(map);

        assertThrows(JobExecutionException.class, () -> job.execute(ctx));
    }

    @Test
    void failure_isRefiredWhileUnderLimit() {
        when(ctx.getMergedJobDataMap()).thenReturn(validData());
        when(ctx.getRefireCount()).thenReturn(0);
        when(service.sendReminders(anyInt(), anyInt())).thenThrow(new IllegalStateException("boom"));

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(ctx));

        assertTrue(ex.refireImmediately());
    }

    @Test
    void failure_givesUpAfterMaxRefires_noInfiniteLoop() {
        when(ctx.getMergedJobDataMap()).thenReturn(validData());
        when(ctx.getRefireCount()).thenReturn(ReadingReminderJob.MAX_REFIRES);
        when(service.sendReminders(anyInt(), anyInt())).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> job.execute(ctx));   // помилка зафіксована в логу, повтору немає
    }
}