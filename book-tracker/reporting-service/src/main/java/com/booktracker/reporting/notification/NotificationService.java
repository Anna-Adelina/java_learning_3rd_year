package com.booktracker.reporting.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Імітація відправки листа-нагадування (власна копія: сервіси не діляться кодом, крім common). */
@Slf4j
@Service
public class NotificationService {

    public record SentEmail(String recipient, String subject, String threadName) {
    }

    private static final int MAX_KEPT = 100;

    private final Deque<SentEmail> sent = new ConcurrentLinkedDeque<>();

    public void sendReadingReminderEmail(String recipient, String bookTitle) {
        SentEmail email = new SentEmail(recipient, "Reminder: keep reading " + bookTitle,
                Thread.currentThread().getName());
        sent.addLast(email);
        while (sent.size() > MAX_KEPT) {
            sent.pollFirst();
        }
        log.info("Reminder sent to {}: {}", recipient, email.subject());
    }

    public List<SentEmail> sentEmails() {
        return List.copyOf(sent);
    }
}
