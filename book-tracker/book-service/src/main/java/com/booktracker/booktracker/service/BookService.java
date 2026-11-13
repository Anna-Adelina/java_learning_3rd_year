package com.booktracker.booktracker.service;

import com.booktracker.booktracker.aop.Audited;
import com.booktracker.booktracker.aop.RetryOnTransientFailure;
import com.booktracker.booktracker.aop.SecurityUtils;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.event.BookEvent;
import com.booktracker.booktracker.event.BookEventType;
import com.booktracker.booktracker.mapper.BookMapper;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.repository.BookRepository;
import com.booktracker.booktracker.service.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;
    private final ApplicationEventPublisher eventPublisher;

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

    @Transactional
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RetryOnTransientFailure(maxAttempts = 3)
    @Audited(action = "BOOK_CREATED")
    public BookDto create(BookDto dto) {
        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Author not found: id=" + dto.getAuthorId()));

        Book book = bookMapper.toEntity(dto);
        book.setId(null);
        book.setAuthor(author);

        Book saved = bookRepository.saveAndFlush(book);
        Book refreshed = bookRepository.findById(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found after save: id=" + saved.getId()));

        BookDto result = bookMapper.toDto(refreshed);
        publish(BookEventType.BOOK_CREATED, result.getId(), result.getTitle());
        return result;
    }

    /** Тепер одна транзакція на весь пакет: або всі книги + всі події, або нічого. */
    @Transactional
    public List<BookDto> createAll(List<BookDto> dtos) {
        List<BookDto> created = new ArrayList<>();
        for (BookDto dto : dtos) {
            created.add(this.create(dto));
        }
        return created;
    }

    @Transactional
    public BookDto update(Long id, BookDto dto) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: id=" + id));

        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Author not found: id=" + dto.getAuthorId()));

        // перевіряємо ДО перезапису статусу: подію шлемо лише в момент переходу в FINISHED
        boolean justFinished = existing.getReadingStatus() != ReadingStatus.FINISHED
                && dto.getReadingStatus() == ReadingStatus.FINISHED;

        existing.setTitle(dto.getTitle());
        existing.setAuthor(author);
        existing.setReadingStatus(dto.getReadingStatus());
        existing.setTotalPages(dto.getTotalPages());
        existing.setPagesRead(dto.getPagesRead());

        Book saved = bookRepository.save(existing);
        if (justFinished) {
            publish(BookEventType.BOOK_FINISHED, saved.getId(), saved.getTitle());
        }
        return bookMapper.toDto(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "BOOK_DELETED")
    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: id=" + id));
        bookRepository.delete(book);
        publish(BookEventType.BOOK_DELETED, id, book.getTitle());
    }

    private void publish(BookEventType type, Long bookId, String title) {
        eventPublisher.publishEvent(BookEvent.of(type, bookId, title, SecurityUtils.currentUsername()));
    }
}