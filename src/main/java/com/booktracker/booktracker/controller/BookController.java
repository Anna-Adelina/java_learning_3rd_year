package com.booktracker.booktracker.controller;

import com.booktracker.booktracker.annotation.CurrentUsername;
import com.booktracker.booktracker.annotation.PostCreated;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookDto> findAll() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    public BookDto findById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @PostCreated
    public BookDto create(@Valid @RequestBody BookDto dto, @CurrentUsername String username) {
        dto.setCreatedBy(username);
        return bookService.create(dto);
    }

    @PutMapping("/{id}")
    public BookDto update(@PathVariable Long id, @Valid @RequestBody BookDto dto) {
        return bookService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }
}