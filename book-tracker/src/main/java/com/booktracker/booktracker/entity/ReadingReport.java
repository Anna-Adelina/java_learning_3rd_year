package com.booktracker.booktracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_type", nullable = false)
    private String periodType;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_books", nullable = false)
    private int totalBooks;

    @Column(name = "planned_count", nullable = false)
    private int plannedCount;

    @Column(name = "reading_count", nullable = false)
    private int readingCount;

    @Column(name = "finished_count", nullable = false)
    private int finishedCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}