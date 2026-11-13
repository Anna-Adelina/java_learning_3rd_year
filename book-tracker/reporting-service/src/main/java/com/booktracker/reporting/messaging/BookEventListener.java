package com.booktracker.reporting.messaging;

import com.booktracker.booktracker.event.BookEvent;
import com.booktracker.reporting.config.RabbitConfig;
import com.booktracker.reporting.service.BookActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventListener {

    private final BookActivityService activityService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onBookEvent(BookEvent event) {
        try {
            activityService.record(event);
        } catch (DataIntegrityViolationException ex) {
            // Два однакових eventId прийшли одночасно, спрацював unique: рядок уже є, це дублікат.
            log.info("Duplicate event {} ignored (unique constraint)", event.eventId());
        }
    }
}