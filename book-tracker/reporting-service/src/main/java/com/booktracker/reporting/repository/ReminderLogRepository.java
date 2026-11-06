package com.booktracker.reporting.repository;

import com.booktracker.reporting.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {
}
