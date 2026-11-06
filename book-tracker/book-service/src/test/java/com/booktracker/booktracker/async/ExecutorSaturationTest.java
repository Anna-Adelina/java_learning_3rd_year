package com.booktracker.booktracker.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ExecutorSaturationTest.TinyExecutorConfig.class)
class ExecutorSaturationTest {

    @Configuration
    @EnableAsync
    @Import(BookInsightsService.class)
    static class TinyExecutorConfig {

        // Навмисно крихітний виконавець: 1 потік, без черги
        @Bean(name = AsyncConfig.EXECUTOR_BEAN)
        ThreadPoolTaskExecutor bookTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(0);
            executor.setThreadNamePrefix("tiny-");
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
            return executor;
        }
    }

    @MockitoBean
    private ExternalBookInfoClient client;

    @Autowired
    private BookInsightsService insights;

    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void freeWorkerThread() {
        release.countDown();
    }

    @Test
    void moreTasksThanExecutorCanHold_overflowIsRejectedWithTaskRejectedException() throws Exception {
        // Arrange: перше завдання займає єдиний потік і "зависає"
        CountDownLatch started = new CountDownLatch(1);
        when(client.fetchRating(1L)).thenAnswer(inv -> {
            started.countDown();
            release.await(10, TimeUnit.SECONDS);
            return 4.5;
        });
        insights.fetchRating(1L);
        assertTrue(started.await(3, TimeUnit.SECONDS), "first task should be running");

        // Act + Assert: для другого нема ні вільного потоку, ні місця в черзі
        assertThrows(TaskRejectedException.class, () -> insights.fetchRating(1L));
    }
}