package com.booktracker.reporting.service;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.reporting.client.BookClient;
import com.booktracker.reporting.entity.ReminderLog;
import com.booktracker.reporting.notification.NotificationService;
import com.booktracker.reporting.repository.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadingReminderService {

    private final BookClient bookClient;
    private final ReminderLogRepository reminderLogRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public int sendReminders(int remindEveryDays, int batchLimit) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.minusDays(remindEveryDays);

        List<BookDto> reading = bookClient.findAll().stream()
                .filter(b -> b.getReadingStatus() == ReadingStatus.READING)
                .filter(b -> b.getId() != null && b.getCreatedBy() != null)
                .sorted(Comparator.comparing(BookDto::getId))
                .toList();

        int sent = 0;
        for (BookDto book : reading) {
            if (sent >= batchLimit) {
                break;
            }
            Optional<ReminderLog> existing = reminderLogRepository.findById(book.getId());
            if (existing.isPresent() && !existing.get().getLastRemindedAt().isBefore(threshold)) {
                continue;   // нагадували нещодавно
            }
            notificationService.sendReadingReminderEmail(book.getCreatedBy(), book.getTitle());
            ReminderLog entry = existing.orElseGet(() -> ReminderLog.builder().bookId(book.getId()).build());
            entry.setLastRemindedAt(now);
            reminderLogRepository.save(entry);
            sent++;
        }
        return sent;
    }
}
