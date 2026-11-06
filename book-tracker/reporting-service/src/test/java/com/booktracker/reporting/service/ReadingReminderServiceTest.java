package com.booktracker.reporting.service;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.reporting.client.BookClient;
import com.booktracker.reporting.entity.ReminderLog;
import com.booktracker.reporting.notification.NotificationService;
import com.booktracker.reporting.repository.ReminderLogRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingReminderServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Mock
    BookClient bookClient;

    @Mock
    ReminderLogRepository reminderLogRepository;

    @Mock
    NotificationService notificationService;

    ReadingReminderService service;

    @BeforeEach
    void setUp() {
        service = new ReadingReminderService(bookClient, reminderLogRepository, notificationService, CLOCK);
    }

    private static BookDto reading(long id, String createdBy) {
        return BookDto.builder().id(id).title("Book " + id).authorId(1L)
                .readingStatus(ReadingStatus.READING).createdBy(createdBy).build();
    }

    @Test
    void sendsReminder_forReadingBookNeverRemindedBefore_andLogsIt() {
        when(bookClient.findAll()).thenReturn(List.of(reading(1, "anna")));
        when(reminderLogRepository.findById(1L)).thenReturn(Optional.empty());

        int sent = service.sendReminders(7, 100);

        assertEquals(1, sent);
        verify(notificationService).sendReadingReminderEmail("anna", "Book 1");
        ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getBookId());
        assertEquals(NOW, captor.getValue().getLastRemindedAt());
    }

    @Test
    void skipsBooksRemindedRecently() {
        when(bookClient.findAll()).thenReturn(List.of(reading(1, "anna")));
        when(reminderLogRepository.findById(1L))
                .thenReturn(Optional.of(new ReminderLog(1L, NOW.minusDays(2))));

        assertEquals(0, service.sendReminders(7, 100));

        verify(notificationService, never()).sendReadingReminderEmail(any(), any());
        verify(reminderLogRepository, never()).save(any());
    }

    @Test
    void remindsAgain_whenLastReminderIsOlderThanThreshold() {
        when(bookClient.findAll()).thenReturn(List.of(reading(1, "anna")));
        when(reminderLogRepository.findById(1L))
                .thenReturn(Optional.of(new ReminderLog(1L, NOW.minusDays(8))));

        assertEquals(1, service.sendReminders(7, 100));
    }

    @Test
    void ignoresNonReadingBooksAndBooksWithoutOwner() {
        BookDto planned = BookDto.builder().id(2L).title("P").authorId(1L)
                .readingStatus(ReadingStatus.PLANNED).createdBy("anna").build();
        when(bookClient.findAll()).thenReturn(List.of(planned, reading(3, null)));

        assertEquals(0, service.sendReminders(7, 100));
    }

    @Test
    void respectsBatchLimit() {
        when(bookClient.findAll()).thenReturn(List.of(reading(1, "a"), reading(2, "b"), reading(3, "c")));
        when(reminderLogRepository.findById(1L)).thenReturn(Optional.empty());
        when(reminderLogRepository.findById(2L)).thenReturn(Optional.empty());

        assertEquals(2, service.sendReminders(7, 2));
    }
}
