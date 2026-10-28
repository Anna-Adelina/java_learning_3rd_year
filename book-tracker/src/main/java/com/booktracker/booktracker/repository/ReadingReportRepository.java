package com.booktracker.booktracker.repository;

import com.booktracker.booktracker.entity.ReadingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ReadingReportRepository extends JpaRepository<ReadingReport, Long> {

    boolean existsByPeriodTypeAndReportDate(String periodType, LocalDate reportDate);
}