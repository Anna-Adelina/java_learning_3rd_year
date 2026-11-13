package com.booktracker.booktracker.messaging;

import com.booktracker.booktracker.config.RabbitConfig;
import com.booktracker.booktracker.event.BookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** Спрацьовує ТІЛЬКИ після commit. При rollback цей метод не викликається взагалі. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BookEvent event) {
        String routingKey = event.type().name().toLowerCase().replace('_', '.'); // BOOK_CREATED -> book.created
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        log.info("Published {} bookId={} eventId={}", event.type(), event.bookId(), event.eventId());
    }
}