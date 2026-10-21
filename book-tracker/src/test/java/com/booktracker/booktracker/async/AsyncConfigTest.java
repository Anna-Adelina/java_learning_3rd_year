package com.booktracker.booktracker.async;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AsyncConfigTest.TestConfig.class)
class AsyncConfigTest {

    @Configuration
    @Import(AsyncConfig.class)
    static class TestConfig {

        @Bean
        ProbeService probeService() {
            return new ProbeService();
        }
    }

    static class ProbeService {

        @Async(AsyncConfig.EXECUTOR_BEAN)
        public CompletableFuture<String> currentThreadName() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }

    @Autowired
    private ProbeService probeService;

    @Autowired
    @Qualifier(AsyncConfig.EXECUTOR_BEAN)
    private ThreadPoolTaskExecutor executor;

    @Test
    void asyncMethod_runsInThreadWithConfiguredPrefix() throws Exception {
        // Act
        String threadName = probeService.currentThreadName().get(2, TimeUnit.SECONDS);

        // Assert
        assertTrue(threadName.startsWith("book-async-"), "actual thread: " + threadName);
        assertNotEquals(Thread.currentThread().getName(), threadName);
    }

    @Test
    void asyncMethod_isNotRunInDefaultOrSimpleExecutor() throws Exception {
        // Act
        String threadName = probeService.currentThreadName().get(2, TimeUnit.SECONDS);

        // Assert
        assertFalse(threadName.startsWith("task-"), "Boot default executor was used: " + threadName);
        assertFalse(threadName.startsWith("SimpleAsyncTaskExecutor"), "SimpleAsyncTaskExecutor was used: " + threadName);
    }

    @Test
    void executor_hasExplicitBoundedConfiguration() {
        // Assert
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(10, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        assertEquals("book-async-", executor.getThreadNamePrefix());
    }
}