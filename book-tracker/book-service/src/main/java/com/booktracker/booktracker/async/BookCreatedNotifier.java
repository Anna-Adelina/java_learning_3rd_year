package com.booktracker.booktracker.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookCreatedNotifier {

    private final NotificationService notificationService;

    public void notifyBookCreated(String recipient, String bookTitle) {
        try {
            notificationService.sendBookCreatedEmail(recipient, bookTitle);
        } catch (TaskRejectedException ex) {
            log.warn("Notification for book '{}' dropped: executor is saturated", bookTitle);
        }
    }
}