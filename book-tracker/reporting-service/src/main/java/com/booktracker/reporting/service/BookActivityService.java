package com.booktracker.reporting.service;

import com.booktracker.booktracker.event.BookEvent;
import com.booktracker.reporting.entity.BookActivity;
import com.booktracker.reporting.repository.BookActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookActivityService {

    private final BookActivityRepository repository;
    private final Clock clock;

    /** Повторна обробка тієї самої події нічого не змінює (ідемпотентність). */
    @Transactional
    public void record(BookEvent event) {
        String eventId = event.eventId().toString();
        if (repository.existsByEventId(eventId)) {
            log.info("Duplicate event {} ignored", eventId);
            return;
        }

        BookActivity activity = BookActivity.builder()
                .eventId(eventId)
                .eventType(event.type().name())
                .bookId(event.bookId())
                .title(event.title())
                .username(event.username())
                .occurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .processedAt(LocalDateTime.now(clock))
                .build();
        repository.save(activity);
        log.info("Recorded {} bookId={} eventId={}", event.type(), event.bookId(), event.eventId());
    }
}