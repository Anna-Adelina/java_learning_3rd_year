package com.booktracker.booktracker.async;

import com.booktracker.booktracker.dto.BookDetailsDto;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.service.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(BookDetailsServiceTest.TestConfig.class)
@TestPropertySource(properties = "app.async.details-timeout-ms=1000")
class BookDetailsServiceTest {

    @Configuration
    @Import({AsyncConfig.class, BookInsightsService.class, BookDetailsService.class})
    static class TestConfig {
    }

    @MockitoBean
    private ExternalBookInfoClient client;

    @MockitoBean
    private BookService bookService;

    @Autowired
    private BookDetailsService detailsService;

    private final CountDownLatch release = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        when(bookService.findById(1L)).thenReturn(
                BookDto.builder().id(1L).title("1984").authorId(7L).build());
    }

    @AfterEach
    void releaseBlockedThreads() {
        release.countDown();
    }

    @Test
    void getDetails_twoIndependentCalls_runConcurrentlyAndAreCombined() throws Exception {
        // Arrange: бар'єр на двох учасників проходить лише якщо ОБИДВА виклики виконуються одночасно
        CyclicBarrier barrier = new CyclicBarrier(2);
        when(client.fetchRating(1L)).thenAnswer(inv -> {
            barrier.await(3, TimeUnit.SECONDS);
            return 4.5;
        });
        when(client.fetchAuthorBio(7L)).thenAnswer(inv -> {
            barrier.await(3, TimeUnit.SECONDS);
            return "Eric Blair";
        });

        // Act
        BookDetailsDto result = detailsService.getDetails(1L);

        // Assert: якби виклики йшли по черзі, бар'єр не пройшов би і частина була б null
        assertFalse(result.partial());
        assertEquals("1984", result.title());
        assertEquals(4.5, result.rating());
        assertEquals("Eric Blair", result.authorBio());
    }

    @Test
    void getDetails_oneCallFails_returnsPartialResultInsteadOfFailing() {
        // Arrange
        when(client.fetchRating(1L)).thenThrow(new IllegalStateException("rating service down"));
        when(client.fetchAuthorBio(7L)).thenReturn("Eric Blair");

        // Act: викликаючий код не отримує винятку
        BookDetailsDto result = detailsService.getDetails(1L);

        // Assert
        assertTrue(result.partial());
        assertNull(result.rating());
        assertEquals("Eric Blair", result.authorBio());
    }

    @Test
    void getDetails_slowCall_stopsWaitingAfterTimeout() {
        // Arrange: рейтинг "зависає", поки тест не відпустить його
        when(client.fetchRating(1L)).thenAnswer(inv -> {
            release.await(10, TimeUnit.SECONDS);
            return 4.5;
        });
        when(client.fetchAuthorBio(7L)).thenReturn("Eric Blair");

        // Act
        long start = System.nanoTime();
        BookDetailsDto result = detailsService.getDetails(1L);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        // Assert: чекали близько таймауту (1 с), а не 10 с
        assertTrue(elapsed.toMillis() < 5000, "waited too long: " + elapsed);
        assertTrue(result.partial());
        assertNull(result.rating());
        assertEquals("Eric Blair", result.authorBio());
    }
}