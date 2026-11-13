package com.booktracker.reporting.service;

import com.booktracker.booktracker.event.BookEvent;
import com.booktracker.booktracker.event.BookEventType;
import com.booktracker.reporting.entity.BookActivity;
import com.booktracker.reporting.repository.BookActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookActivityServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private BookActivityRepository repository;

    private BookActivityService service;

    @BeforeEach
    void setUp() {
        service = new BookActivityService(repository, CLOCK);
    }

    private BookEvent event() {
        return new BookEvent(EVENT_ID, BookEventType.BOOK_CREATED, 1L, "1984", "anna",
                Instant.parse("2026-11-12T09:59:00Z"));
    }

    @Test
    void record_newEvent_savesActivity() {
        when(repository.existsByEventId(EVENT_ID.toString())).thenReturn(false);

        service.record(event());

        ArgumentCaptor<BookActivity> captor = ArgumentCaptor.forClass(BookActivity.class);
        verify(repository).save(captor.capture());
        BookActivity saved = captor.getValue();
        assertEquals(EVENT_ID.toString(), saved.getEventId());
        assertEquals("BOOK_CREATED", saved.getEventType());
        assertEquals(1L, saved.getBookId());
        assertEquals("1984", saved.getTitle());
        assertEquals("anna", saved.getUsername());
        assertEquals(LocalDateTime.of(2026, 11, 12, 9, 59), saved.getOccurredAt());
        assertEquals(LocalDateTime.of(2026, 11, 12, 10, 0), saved.getProcessedAt());
    }

    @Test
    void record_sameEventDeliveredTwice_savesOnlyOnce() {
        BookEvent event = event();
        // перший раз такого eventId ще нема, вдруге він уже є
        when(repository.existsByEventId(EVENT_ID.toString())).thenReturn(false, true);

        service.record(event);
        service.record(event);

        verify(repository, times(1)).save(any(BookActivity.class));
    }
}