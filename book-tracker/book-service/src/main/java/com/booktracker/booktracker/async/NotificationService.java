package com.booktracker.booktracker.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
public class NotificationService {

    public record SentEmail(String recipient, String subject, String threadName) {
    }

    private static final int MAX_KEPT = 100;

    private final long simulatedDelayMs;
    private final Deque<SentEmail> sent = new ConcurrentLinkedDeque<>();

    public NotificationService(@Value("${app.async.simulated-delay-ms:1000}") long simulatedDelayMs) {
        this.simulatedDelayMs = simulatedDelayMs;
    }

    /**
     * Fire-and-forget: викликаючий код не чекає на "відправку листа".
     */
    @Async(AsyncConfig.EXECUTOR_BEAN)
    public void sendBookCreatedEmail(String recipient, String bookTitle) {
        if (bookTitle == null || bookTitle.isBlank()) {
            throw new IllegalArgumentException("Cannot send notification: book title is blank");
        }
        SimulatedLatency.pause(simulatedDelayMs);

        SentEmail email = new SentEmail(recipient, "Book added: " + bookTitle,
                Thread.currentThread().getName());
        sent.addLast(email);
        while (sent.size() > MAX_KEPT) {
            sent.pollFirst();
        }
        log.info("Email sent to {}: {} (thread {})", email.recipient(), email.subject(), email.threadName());
    }

    public List<SentEmail> sentEmails() {
        return List.copyOf(sent);
    }
}