package com.booktracker.booktracker.service;

import com.booktracker.booktracker.entity.ReadingReport;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.repository.BookRepository;
import com.booktracker.booktracker.repository.ReadingReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReadingReportService {

    private final BookRepository bookRepository;
    private final ReadingReportRepository reportRepository;
    private final Clock clock;

    /** @return true, якщо звіт створено; false, якщо вже існує або книг замало */
    @Transactional
    public boolean generate(String periodType, int minBooks) {
        LocalDate today = LocalDate.now(clock);
        if (reportRepository.existsByPeriodTypeAndReportDate(periodType, today)) {
            return false;   // ідемпотентність
        }
        long total = bookRepository.count();
        if (total < minBooks) {
            return false;
        }
        reportRepository.save(ReadingReport.builder()
                .periodType(periodType)
                .reportDate(today)
                .totalBooks((int) total)
                .plannedCount((int) bookRepository.countByReadingStatus(ReadingStatus.PLANNED))
                .readingCount((int) bookRepository.countByReadingStatus(ReadingStatus.READING))
                .finishedCount((int) bookRepository.countByReadingStatus(ReadingStatus.FINISHED))
                .generatedAt(LocalDateTime.now(clock))
                .build());
        return true;
    }
}