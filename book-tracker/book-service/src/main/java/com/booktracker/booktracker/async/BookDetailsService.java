package com.booktracker.booktracker.async;

import com.booktracker.booktracker.dto.BookDetailsDto;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BookDetailsService {

    private final BookService bookService;
    private final BookInsightsService insights;
    private final long timeoutMs;

    public BookDetailsService(BookService bookService,
                              BookInsightsService insights,
                              @Value("${app.async.details-timeout-ms:2000}") long timeoutMs) {
        this.bookService = bookService;
        this.insights = insights;
        this.timeoutMs = timeoutMs;
    }

    public BookDetailsDto getDetails(Long bookId) {
        BookDto book = bookService.findById(bookId);

        // Обидва виклики стартують одразу, не чекаючи один на одного
        CompletableFuture<Double> rating = insights.fetchRating(bookId)
                .completeOnTimeout(null, timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("Rating unavailable for book {}: {}", bookId, ex.toString());
                    return null;
                });

        CompletableFuture<String> bio = insights.fetchAuthorBio(book.getAuthorId())
                .completeOnTimeout(null, timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("Author bio unavailable for author {}: {}", book.getAuthorId(), ex.toString());
                    return null;
                });

        // Об'єднуємо два незалежні результати в один
        return rating
                .thenCombine(bio, (r, b) ->
                        new BookDetailsDto(book.getId(), book.getTitle(), r, b, r == null || b == null))
                .join();
    }
}