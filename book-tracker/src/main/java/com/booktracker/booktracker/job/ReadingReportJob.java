package com.booktracker.booktracker.job;

import com.booktracker.booktracker.service.ReadingReportService;
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
public class ReadingReportJob implements Job {

    public static final String KEY_PERIOD_TYPE = "periodType";
    public static final String KEY_MIN_BOOKS = "minBooks";

    private final ReadingReportService service;

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data = ctx.getMergedJobDataMap();
        String periodType;
        int minBooks;
        try {
            periodType = JobParams.string(data, KEY_PERIOD_TYPE);
            minBooks = JobParams.positiveInt(data, KEY_MIN_BOOKS);
        } catch (JobParams.JobParameterException e) {
            throw new JobExecutionException(e, false);
        }
        try {
            boolean created = service.generate(periodType, minBooks);
            log.info("{} report {}", periodType, created ? "generated" : "skipped (already exists or too few books)");
        } catch (RuntimeException e) {
            log.error("{} report failed; will retry at next scheduled run", periodType, e);
        }
    }
}