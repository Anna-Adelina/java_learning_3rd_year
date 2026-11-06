package com.booktracker.reporting.repository;

import com.booktracker.reporting.entity.ReadingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ReadingReportRepository extends JpaRepository<ReadingReport, Long> {

    boolean existsByPeriodTypeAndReportDate(String periodType, LocalDate reportDate);
}
