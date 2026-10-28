package com.booktracker.booktracker.job;

import com.booktracker.booktracker.service.ReadingReportService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingReportJobTest {

    @Mock
    ReadingReportService service;

    @Mock
    JobExecutionContext ctx;

    @InjectMocks
    ReadingReportJob job;

    private JobDataMap validData() {
        JobDataMap map = new JobDataMap();
        map.put(ReadingReportJob.KEY_PERIOD_TYPE, "MONTHLY");
        map.put(ReadingReportJob.KEY_MIN_BOOKS, 1);
        return map;
    }

    @Test
    void readsParamsAndCallsServiceExactlyOnce() throws Exception {
        when(ctx.getMergedJobDataMap()).thenReturn(validData());

        job.execute(ctx);

        verify(service, times(1)).generate("MONTHLY", 1);
    }

    @Test
    void missingPeriodType_throwsExplicitError() {
        JobDataMap map = validData();
        map.remove(ReadingReportJob.KEY_PERIOD_TYPE);
        when(ctx.getMergedJobDataMap()).thenReturn(map);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(ctx));

        assertFalse(ex.refireImmediately());
    }

    @Test
    void failure_isRecordedAndSwallowed_waitingForNextScheduledRun() {
        when(ctx.getMergedJobDataMap()).thenReturn(validData());
        when(service.generate(anyString(), anyInt())).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> job.execute(ctx));   // без винятку й без refire
    }
}