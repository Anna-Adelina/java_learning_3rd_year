package com.booktracker.booktracker.async;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class BookInsightsService {

    private final ExternalBookInfoClient client;

    @Async(AsyncConfig.EXECUTOR_BEAN)
    public CompletableFuture<Double> fetchRating(Long bookId) {
        return CompletableFuture.completedFuture(client.fetchRating(bookId));
    }

    @Async(AsyncConfig.EXECUTOR_BEAN)
    public CompletableFuture<String> fetchAuthorBio(Long authorId) {
        return CompletableFuture.completedFuture(client.fetchAuthorBio(authorId));
    }
}