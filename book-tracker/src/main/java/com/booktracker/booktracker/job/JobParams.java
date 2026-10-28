package com.booktracker.booktracker.job;

import org.quartz.JobDataMap;

public final class JobParams {

    private JobParams() {
    }

    public static class JobParameterException extends IllegalArgumentException {
        public JobParameterException(String key, String problem) {
            super("Invalid job parameter '" + key + "': " + problem);
        }
    }

    public static String string(JobDataMap map, String key) {
        Object v = map.get(key);
        if (v instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new JobParameterException(key, "expected non-blank string, got " + v);
    }

    public static int positiveInt(JobDataMap map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n && n.intValue() > 0) {
            return n.intValue();
        }
        throw new JobParameterException(key, "expected positive number, got " + v);
    }
}