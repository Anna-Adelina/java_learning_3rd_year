package com.booktracker.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Колись це було поле Book.lastRemindedAt; тепер це власна таблиця reporting-service (без FK на чужу БД). */
@Entity
@Table(name = "reminder_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "last_reminded_at", nullable = false)
    private LocalDateTime lastRemindedAt;
}
