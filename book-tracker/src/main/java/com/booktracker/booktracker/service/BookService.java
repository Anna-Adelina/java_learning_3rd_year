package com.booktracker.booktracker.service;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.mapper.BookMapper;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.repository.BookRepository;
import com.booktracker.booktracker.service.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;

    public List<BookDto> findAll() {
        return bookRepository.findAll()
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public BookDto findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: id=" + id));
        return bookMapper.toDto(book);
    }

    public BookDto create(BookDto dto) {
        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Author not found: id=" + dto.getAuthorId()));

        Book book = bookMapper.toEntity(dto);
        book.setId(null);
        book.setAuthor(author);

        Book saved = bookRepository.saveAndFlush(book);
        Book refreshed = bookRepository.findById(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found after save: id=" + saved.getId()));

        return bookMapper.toDto(refreshed);
    }

    public BookDto update(Long id, BookDto dto) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: id=" + id));

        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Author not found: id=" + dto.getAuthorId()));

        existing.setTitle(dto.getTitle());
        existing.setAuthor(author);
        existing.setReadingStatus(dto.getReadingStatus());
        existing.setTotalPages(dto.getTotalPages());
        existing.setPagesRead(dto.getPagesRead());

        Book saved = bookRepository.save(existing);
        return bookMapper.toDto(saved);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new EntityNotFoundException("Book not found: id=" + id);
        }
        bookRepository.deleteById(id);
    }

}