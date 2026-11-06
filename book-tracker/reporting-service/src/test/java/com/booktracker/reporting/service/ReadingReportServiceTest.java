package com.booktracker.reporting.service;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.reporting.client.BookClient;
import com.booktracker.reporting.entity.ReadingReport;
import com.booktracker.reporting.repository.ReadingReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Mock
    BookClient bookClient;

    @Mock
    ReadingReportRepository reportRepository;

    ReadingReportService service;

    @BeforeEach
    void setUp() {
        service = new ReadingReportService(bookClient, reportRepository, CLOCK);
    }

    private static BookDto book(long id, ReadingStatus status) {
        return BookDto.builder().id(id).title("T" + id).authorId(1L).readingStatus(status).build();
    }

    @Test
    void generate_countsBooksByStatus_andSavesReport() {
        when(reportRepository.existsByPeriodTypeAndReportDate("WEEKLY", TODAY)).thenReturn(false);
        when(bookClient.findAll()).thenReturn(List.of(
                book(1, ReadingStatus.PLANNED), book(2, ReadingStatus.READING),
                book(3, ReadingStatus.READING), book(4, ReadingStatus.FINISHED)));

        assertTrue(service.generate("WEEKLY", 1));

        ArgumentCaptor<ReadingReport> captor = ArgumentCaptor.forClass(ReadingReport.class);
        verify(reportRepository).save(captor.capture());
        ReadingReport saved = captor.getValue();
        assertEquals(4, saved.getTotalBooks());
        assertEquals(1, saved.getPlannedCount());
        assertEquals(2, saved.getReadingCount());
        assertEquals(1, saved.getFinishedCount());
        assertEquals(TODAY, saved.getReportDate());
    }

    @Test
    void generate_isIdempotent_andDoesNotCallBookService() {
        when(reportRepository.existsByPeriodTypeAndReportDate("WEEKLY", TODAY)).thenReturn(true);

        assertFalse(service.generate("WEEKLY", 1));

        verifyNoInteractions(bookClient);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void generate_skipsWhenTooFewBooks() {
        when(reportRepository.existsByPeriodTypeAndReportDate("MONTHLY", TODAY)).thenReturn(false);
        when(bookClient.findAll()).thenReturn(List.of(book(1, ReadingStatus.PLANNED)));

        assertFalse(service.generate("MONTHLY", 5));

        verify(reportRepository, never()).save(any());
    }
}
