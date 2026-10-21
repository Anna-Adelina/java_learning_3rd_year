package com.booktracker.booktracker.async;

import com.booktracker.booktracker.async.NotificationService.SentEmail;
import com.booktracker.booktracker.async.RecordingAsyncExceptionHandler.AsyncFailure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(NotificationServiceTest.TestConfig.class)
@TestPropertySource(properties = "app.async.simulated-delay-ms=500")
class NotificationServiceTest {

    @Configuration
    @Import({AsyncConfig.class, NotificationService.class})
    static class TestConfig {
    }

    @Autowired
    private NotificationService service;

    @Autowired
    private RecordingAsyncExceptionHandler exceptionHandler;

    @Test
    void sendBookCreatedEmail_returnsImmediately_andCompletesLaterInExecutorThread() {
        // Arrange
        int before = service.sentEmails().size();

        // Act
        service.sendBookCreatedEmail("anna", "1984");

        // Assert: керування вже повернулось, а "лист" ще не відправлено
        assertEquals(before, service.sentEmails().size());

        // ...і він завершується пізніше, у потоці нашого виконавця
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            List<SentEmail> sent = service.sentEmails();
            assertEquals(before + 1, sent.size());
            SentEmail last = sent.get(sent.size() - 1);
            assertEquals("Book added: 1984", last.subject());
            assertTrue(last.threadName().startsWith("book-async-"), "thread: " + last.threadName());
        });
    }

    @Test
    void sendBookCreatedEmail_failureInsideAsyncVoid_isCapturedByUncaughtExceptionHandler() {
        // Arrange
        int before = exceptionHandler.failures().size();

        // Act: викликаючий код не бачить жодного винятку
        assertDoesNotThrow(() -> service.sendBookCreatedEmail("anna", " "));

        // Assert: збій не загубився, його зафіксував обробник
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            List<AsyncFailure> failures = exceptionHandler.failures();
            assertEquals(before + 1, failures.size());
            AsyncFailure last = failures.get(failures.size() - 1);
            assertEquals("NotificationService.sendBookCreatedEmail", last.method());
            assertTrue(last.message().contains("title is blank"));
        });
    }
}