package com.booktracker.booktracker.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String EXECUTOR_BEAN = "bookTaskExecutor";
    public static final String THREAD_PREFIX = "book-async-";

    @Bean(name = EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor bookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix(THREAD_PREFIX);
        // Явне рішення: при переповненні кидаємо виняток (Spring загорне його в TaskRejectedException)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    @Bean
    public RecordingAsyncExceptionHandler asyncExceptionHandler() {
        return new RecordingAsyncExceptionHandler();
    }

    @Override
    public Executor getAsyncExecutor() {
        return bookTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler();
    }
}