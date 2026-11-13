package com.booktracker.reporting.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "book.events";
    public static final String QUEUE = "reporting.book-events";

    /** Той самий обмінник і з тими ж параметрами, що в producer: оголошення ідемпотентне. */
    @Bean
    public TopicExchange bookEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** durable: черга (і її повідомлення) переживають зупинку consumer і перезапуск брокера. */
    @Bean
    public Queue bookEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    /** book.created, book.finished, book.deleted: усі з одним префіксом. */
    @Bean
    public Binding bookEventsBinding(Queue bookEventsQueue, TopicExchange bookEventsExchange) {
        return BindingBuilder.bind(bookEventsQueue).to(bookEventsExchange).with("book.*");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}