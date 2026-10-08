package com.booktracker.booktracker.repository;

import com.booktracker.booktracker.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}