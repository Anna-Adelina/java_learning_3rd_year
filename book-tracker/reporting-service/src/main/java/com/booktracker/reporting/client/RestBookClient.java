package com.booktracker.reporting.client;

import com.booktracker.booktracker.dto.BookDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Реалізація BookClient: GET /api/books у book-service. Токен додає interceptor з BookClientConfig. */
@Component
public class RestBookClient implements BookClient {

    private final RestClient restClient;

    public RestBookClient(@Qualifier("bookRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<BookDto> findAll() {
        List<BookDto> books = restClient.get()
                .uri("/api/books")
                .retrieve()
                .body(new ParameterizedTypeReference<List<BookDto>>() {
                });
        return books == null ? List.of() : books;
    }
}
