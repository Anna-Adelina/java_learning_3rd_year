package com.booktracker.reporting.client;

import com.booktracker.booktracker.dto.BookDto;

import java.util.List;

/**
 * Єдине місце, через яке reporting-service отримує книжки з book-service.
 * Реалізація (REST + токен) з'явиться на кроці 4b; зараз сервіси залежать лише від цього інтерфейсу.
 */
public interface BookClient {

    List<BookDto> findAll();
}
