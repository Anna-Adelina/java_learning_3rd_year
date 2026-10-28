package com.booktracker.booktracker.service;

import com.booktracker.booktracker.async.NotificationService;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingReminderService {

    private final BookRepository bookRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public int sendReminders(int remindEveryDays, int batchLimit) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Book> due = bookRepository.findDueForReminder(
                ReadingStatus.READING, now.minusDays(remindEveryDays), PageRequest.of(0, batchLimit));
        for (Book book : due) {
            notificationService.sendReadingReminderEmail(book.getCreatedBy(), book.getTitle());
            book.setLastRemindedAt(now);   // dirty checking збереже зміну при коміті
        }
        return due.size();
    }
}