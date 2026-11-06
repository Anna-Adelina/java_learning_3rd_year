package com.booktracker.booktracker.aop;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.mapper.BookMapper;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.repository.BookRepository;
import com.booktracker.booktracker.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SelfInvocationTest.TestConfig.class)
class SelfInvocationTest {

    @Configuration
    @EnableAspectJAutoProxy
    @Import({AuditedAspect.class, AuditTrail.class, RetryOnTransientFailureAspect.class})
    static class TestConfig {

        @Bean
        BookRepository bookRepository() {
            return mock(BookRepository.class);
        }

        @Bean
        AuthorRepository authorRepository() {
            return mock(AuthorRepository.class);
        }

        @Bean
        BookMapper bookMapper() {
            return mock(BookMapper.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        BookService bookService(BookRepository books, AuthorRepository authors, BookMapper mapper) {
            return new BookService(books, authors, mapper);
        }
    }

    @Autowired
    private BookService bookService;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private AuditTrail auditTrail;

    private BookDto dto;
    private Book saved;

    @BeforeEach
    void setUp() {
        reset(bookRepository, authorRepository, bookMapper);
        Author author = Author.builder().id(1L).firstName("George").lastName("Orwell").build();
        Book mapped = Book.builder().title("1984").build();
        saved = Book.builder().id(10L).title("1984").build();
        dto = BookDto.builder().id(10L).title("1984").authorId(1L)
                .readingStatus(ReadingStatus.PLANNED).build();

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookMapper.toEntity(any(BookDto.class))).thenReturn(mapped);
        doReturn(saved).when(bookRepository).saveAndFlush(any(Book.class));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(saved));
        when(bookMapper.toDto(saved)).thenReturn(dto);
    }

    // ---------- виклик ззовні: advice спрацьовує ----------

    @Test
    void create_calledFromOutside_isAudited() {
        // Arrange
        int before = auditTrail.entries().size();

        // Act
        bookService.create(dto);

        // Assert
        List<AuditEntry> entries = auditTrail.entries();
        assertEquals(before + 1, entries.size());
        assertEquals("BOOK_CREATED", entries.get(entries.size() - 1).action());
    }

    @Test
    void create_calledFromOutside_isRetriedAfterTransientFailure() {
        // Arrange
        doThrow(new TransientDataAccessResourceException("db hiccup"))
                .doReturn(saved)
                .when(bookRepository).saveAndFlush(any(Book.class));

        // Act + Assert
        assertDoesNotThrow(() -> bookService.create(dto));
        verify(bookRepository, times(2)).saveAndFlush(any(Book.class));
    }

    // ---------- self-invocation: advice МОВЧКИ не спрацьовує ----------

    @Test
    void createAll_callsCreateViaThis_isNotAudited() {
        // Arrange
        int before = auditTrail.entries().size();

        // Act
        List<BookDto> result = bookService.createAll(List.of(dto, dto));

        // Assert: метод відпрацював, але жодного запису аудиту
        assertEquals(2, result.size());
        assertEquals(before, auditTrail.entries().size());
    }

    @Test
    void createAll_transientFailureInsideCreate_isNotRetried() {
        // Arrange
        doThrow(new TransientDataAccessResourceException("db hiccup"))
                .doReturn(saved)
                .when(bookRepository).saveAndFlush(any(Book.class));

        // Act + Assert: той самий збій, але повтору немає
        assertThrows(TransientDataAccessResourceException.class,
                () -> bookService.createAll(List.of(dto)));
        verify(bookRepository, times(1)).saveAndFlush(any(Book.class));
    }
}