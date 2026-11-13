package com.booktracker.booktracker.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Подія, яку book-service кладе в брокер, а reporting-service забирає.
 * eventId потрібен для ідемпотентності (сценарій 5): та сама подія вдруге = той самий eventId.
 */
public record BookEvent(
        UUID eventId,
        BookEventType type,
        Long bookId,
        String title,
        String username,
        Instant occurredAt
) {
    public static BookEvent of(BookEventType type, Long bookId, String title, String username) {
        return new BookEvent(UUID.randomUUID(), type, bookId, title, username, Instant.now());
    }
}