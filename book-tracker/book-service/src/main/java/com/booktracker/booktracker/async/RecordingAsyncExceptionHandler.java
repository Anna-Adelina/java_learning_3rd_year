package com.booktracker.booktracker.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Slf4j
public class RecordingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    public record AsyncFailure(String method, String message) {
    }

    private static final int MAX_ENTRIES = 100;

    private final Deque<AsyncFailure> failures = new ArrayDeque<>();

    @Override
    public synchronized void handleUncaughtException(Throwable ex, Method method, Object... params) {
        String name = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        log.error("Uncaught exception in @Async method {}", name, ex);

        if (failures.size() >= MAX_ENTRIES) {
            failures.pollFirst();
        }
        failures.addLast(new AsyncFailure(name, ex.getMessage()));
    }

    public synchronized List<AsyncFailure> failures() {
        return List.copyOf(failures);
    }
}