package com.booktracker.booktracker.messaging;

import com.booktracker.booktracker.event.BookEvent;
import com.booktracker.booktracker.event.BookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Сценарій 2: публікація в брокер лише ПІСЛЯ commit; при rollback/помилці подія не йде. */
@SpringJUnitConfig(BookEventPublisherTransactionTest.TestConfig.class)
class BookEventPublisherTransactionTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:event-tx-test;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
            return new TransactionTemplate(tm);
        }

        @Bean
        RabbitTemplate rabbitTemplate() {
            return mock(RabbitTemplate.class);
        }

        @Bean
        BookEventPublisher bookEventPublisher(RabbitTemplate rabbitTemplate) {
            return new BookEventPublisher(rabbitTemplate);
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        Mockito.reset(rabbitTemplate);
    }

    private BookEvent event() {
        return BookEvent.of(BookEventType.BOOK_CREATED, 1L, "1984", "anna");
    }

    @Test
    void commit_publishesToBrokerOnlyAfterCommit() {
        tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event());
            verifyNoInteractions(rabbitTemplate);   // ще всередині транзакції: у брокері порожньо
        });

        verify(rabbitTemplate).convertAndSend(eq("book.events"), eq("book.created"), any(BookEvent.class));
    }

    @Test
    void rollback_publishesNothing() {
        tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event());
            status.setRollbackOnly();
        });

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void exceptionInsideTransaction_publishesNothing() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event());
            throw new IllegalStateException("boom");
        }));

        verifyNoInteractions(rabbitTemplate);
    }
}