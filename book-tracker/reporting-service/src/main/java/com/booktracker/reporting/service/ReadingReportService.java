package com.booktracker.reporting.service;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.reporting.client.BookClient;
import com.booktracker.reporting.entity.ReadingReport;
import com.booktracker.reporting.repository.ReadingReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingReportService {

    private final BookClient bookClient;
    private final ReadingReportRepository reportRepository;
    private final Clock clock;

    /** @return true, якщо звіт створено; false, якщо вже існує або книг замало */
    @Transactional
    public boolean generate(String periodType, int minBooks) {
        LocalDate today = LocalDate.now(clock);
        if (reportRepository.existsByPeriodTypeAndReportDate(periodType, today)) {
            return false;   // ідемпотентність: до book-service навіть не ходимо
        }
        List<BookDto> books = bookClient.findAll();
        if (books.size() < minBooks) {
            return false;
        }
        reportRepository.save(ReadingReport.builder()
                .periodType(periodType)
                .reportDate(today)
                .totalBooks(books.size())
                .plannedCount(count(books, ReadingStatus.PLANNED))
                .readingCount(count(books, ReadingStatus.READING))
                .finishedCount(count(books, ReadingStatus.FINISHED))
                .generatedAt(LocalDateTime.now(clock))
                .build());
        return true;
    }

    private static int count(List<BookDto> books, ReadingStatus status) {
        return (int) books.stream().filter(b -> b.getReadingStatus() == status).count();
    }
}
