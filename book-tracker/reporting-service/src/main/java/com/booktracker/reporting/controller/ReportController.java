package com.booktracker.reporting.controller;

import com.booktracker.reporting.entity.ReadingReport;
import com.booktracker.reporting.repository.ReadingReportRepository;
import com.booktracker.reporting.service.ReadingReminderService;
import com.booktracker.reporting.service.ReadingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Невеликий API, щоб перевірити роботу сервісу без очікування cron. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReadingReportRepository reportRepository;
    private final ReadingReportService reportService;
    private final ReadingReminderService reminderService;

    @GetMapping
    public List<ReadingReport> list() {
        return reportRepository.findAll();
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestParam(defaultValue = "WEEKLY") String period) {
        return Map.of("period", period, "created", reportService.generate(period, 1));
    }

    @PostMapping("/reminders")
    public Map<String, Integer> reminders() {
        return Map.of("sent", reminderService.sendReminders(7, 100));
    }
}
