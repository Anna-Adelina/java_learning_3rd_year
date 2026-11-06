package com.booktracker.booktracker.async;

public interface ExternalBookInfoClient {

    double fetchRating(Long bookId);

    String fetchAuthorBio(Long authorId);
}