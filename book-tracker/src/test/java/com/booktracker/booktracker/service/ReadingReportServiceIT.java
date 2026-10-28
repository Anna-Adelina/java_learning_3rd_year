package com.booktracker.booktracker.service;

import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingReport;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.repository.BookRepository;
import com.booktracker.booktracker.repository.ReadingReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(ReadingReportService.class)
class ReadingReportServiceIT {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-01T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    ReadingReportService service;
    @Autowired
    ReadingReportRepository reportRepository;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {
        Author author = authorRepository.save(Author.builder().firstName("George").lastName("Orwell").build());
        bookRepository.save(Book.builder().title("A").author(author).readingStatus(ReadingStatus.READING).build());
        bookRepository.save(Book.builder().title("B").author(author).readingStatus(ReadingStatus.FINISHED).build());
        bookRepository.saveAndFlush(Book.builder().title("C").author(author).readingStatus(ReadingStatus.PLANNED).build());
    }

    @Test
    void runningTwiceForSamePeriod_createsExactlyOneReport() {
        assertTrue(service.generate("WEEKLY", 1));
        assertFalse(service.generate("WEEKLY", 1));   // другий запуск нічого не змінює

        assertEquals(1, reportRepository.count());
        ReadingReport report = reportRepository.findAll().get(0);
        assertEquals(3, report.getTotalBooks());
        assertEquals(1, report.getPlannedCount());
        assertEquals(1, report.getReadingCount());
        assertEquals(1, report.getFinishedCount());
    }

    @Test
    void differentPeriodTypesOnSameDay_areSeparateReports() {
        assertTrue(service.generate("WEEKLY", 1));
        assertTrue(service.generate("MONTHLY", 1));
        assertEquals(2, reportRepository.count());
    }

    @Test
    void notEnoughBooks_createsNoReport() {
        assertFalse(service.generate("WEEKLY", 10));
        assertEquals(0, reportRepository.count());
    }
}