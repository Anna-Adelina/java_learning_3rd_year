package com.booktracker.booktracker.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "book.events";

    /** durable = true: обмінник переживає перезапуск брокера. */
    @Bean
    public TopicExchange bookEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Подія летить як JSON, а не як Java-серіалізація (читабельно в панелі, не залежить від класів). */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}