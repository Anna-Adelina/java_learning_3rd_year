package com.booktracker.booktracker.service;

import com.booktracker.booktracker.async.NotificationService;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import(ReadingReminderService.class)
class ReadingReminderServiceIT {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-01T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @MockitoBean
    NotificationService notificationService;

    @Autowired
    ReadingReminderService service;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    AuthorRepository authorRepository;

    private Author author;

    @BeforeEach
    void setUp() {
        author = authorRepository.save(Author.builder().firstName("George").lastName("Orwell").build());
    }

    private Book saveBook(String title, ReadingStatus status, String createdBy, LocalDateTime lastRemindedAt) {
        return bookRepository.saveAndFlush(Book.builder()
                .title(title).author(author).readingStatus(status)
                .createdBy(createdBy).lastRemindedAt(lastRemindedAt).build());
    }

    @Test
    void runningTwiceInARow_sendsReminderOnlyOnce() {
        saveBook("1984", ReadingStatus.READING, "anna", null);

        assertEquals(1, service.sendReminders(7, 100));
        assertEquals(0, service.sendReminders(7, 100));   // другий запуск нічого не змінює

        verify(notificationService, times(1)).sendReadingReminderEmail("anna", "1984");
    }

    @Test
    void ignoresNonReadingBooks_andBooksWithoutOwner() {
        saveBook("Planned", ReadingStatus.PLANNED, "anna", null);
        saveBook("Finished", ReadingStatus.FINISHED, "anna", null);
        saveBook("No owner", ReadingStatus.READING, null, null);

        assertEquals(0, service.sendReminders(7, 100));
        verify(notificationService, never()).sendReadingReminderEmail(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void remindsAgainOnlyAfterConfiguredInterval() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 1, 9, 0);
        saveBook("Old reminder", ReadingStatus.READING, "anna", now.minusDays(8));
        saveBook("Fresh reminder", ReadingStatus.READING, "anna", now.minusDays(2));

        assertEquals(1, service.sendReminders(7, 100));
        verify(notificationService, times(1)).sendReadingReminderEmail("anna", "Old reminder");
    }
}