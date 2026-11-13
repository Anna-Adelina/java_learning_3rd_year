package com.booktracker.booktracker.service;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.mapper.BookMapper;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.repository.BookRepository;
import com.booktracker.booktracker.service.exception.EntityNotFoundException;
import com.booktracker.booktracker.event.BookEvent;
import com.booktracker.booktracker.event.BookEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookService bookService;

    @Test
    void findAll_whenBooksExist_returnsMappedDtoList() {
        // Arrange
        Book book = Book.builder().id(1L).title("1984").readingStatus(ReadingStatus.PLANNED).build();
        BookDto dto = BookDto.builder().id(1L).title("1984").authorId(1L).readingStatus(ReadingStatus.PLANNED).build();
        when(bookRepository.findAll()).thenReturn(List.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);

        // Act
        List<BookDto> result = bookService.findAll();

        // Assert
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void findById_whenBookExists_returnsDto() {
        // Arrange
        Book book = Book.builder().id(1L).title("1984").readingStatus(ReadingStatus.PLANNED).build();
        BookDto dto = BookDto.builder().id(1L).title("1984").authorId(1L).readingStatus(ReadingStatus.PLANNED).build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);

        // Act
        BookDto result = bookService.findById(1L);

        // Assert
        assertEquals(dto, result);
    }

    @Test
    void findById_whenBookNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> bookService.findById(99L));
    }

    @Test
    void create_whenAuthorExists_savesBookWithAuthorAndClearsId() {
        // Arrange
        BookDto inputDto = BookDto.builder().id(999L).title("1984").authorId(1L).readingStatus(ReadingStatus.PLANNED).build();
        Author author = Author.builder().id(1L).firstName("George").lastName("Orwell").build();
        Book mappedEntity = Book.builder().id(999L).title("1984").readingStatus(ReadingStatus.PLANNED).build();
        Book savedEntity = Book.builder().id(1L).title("1984").author(author).readingStatus(ReadingStatus.PLANNED).build();
        BookDto resultDto = BookDto.builder().id(1L).title("1984").authorId(1L).readingStatus(ReadingStatus.PLANNED).build();

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookMapper.toEntity(inputDto)).thenReturn(mappedEntity);
        when(bookRepository.saveAndFlush(any(Book.class))).thenReturn(savedEntity);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(savedEntity));
        when(bookMapper.toDto(savedEntity)).thenReturn(resultDto);

        // Act
        BookDto result = bookService.create(inputDto);

        // Assert
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getId(), "id клієнта має бути обнулений перед збереженням");
        assertEquals(author, captor.getValue().getAuthor());
        assertEquals(resultDto, result);
    }

    @Test
    void create_whenAuthorNotFound_throwsEntityNotFoundException() {
        // Arrange
        BookDto inputDto = BookDto.builder().title("1984").authorId(99L).readingStatus(ReadingStatus.PLANNED).build();
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> bookService.create(inputDto));
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void update_whenBookAndAuthorExist_updatesFieldsAndSaves() {
        // Arrange
        Author author = Author.builder().id(1L).firstName("George").lastName("Orwell").build();
        Book existing = Book.builder().id(1L).title("Old Title").readingStatus(ReadingStatus.PLANNED).build();
        BookDto updateDto = BookDto.builder().title("1984").authorId(1L).readingStatus(ReadingStatus.READING).build();
        BookDto resultDto = BookDto.builder().id(1L).title("1984").authorId(1L).readingStatus(ReadingStatus.READING).build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(existing)).thenReturn(existing);
        when(bookMapper.toDto(existing)).thenReturn(resultDto);

        // Act
        BookDto result = bookService.update(1L, updateDto);

        // Assert
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertEquals("1984", captor.getValue().getTitle());
        assertEquals(ReadingStatus.READING, captor.getValue().getReadingStatus());
        assertEquals(resultDto, result);
    }

    @Test
    void update_whenBookNotFound_throwsEntityNotFoundException() {
        // Arrange
        BookDto updateDto = BookDto.builder().title("1984").authorId(1L).readingStatus(ReadingStatus.PLANNED).build();
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> bookService.update(99L, updateDto));
    }

    @Test
    void delete_whenBookExists_deletesBook() {
        Book book = Book.builder().id(1L).title("1984").build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.delete(1L);

        verify(bookRepository).delete(book);
        verify(eventPublisher).publishEvent(any(BookEvent.class));
    }

    @Test
    void delete_whenBookNotFound_throwsEntityNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bookService.delete(99L));
        verify(bookRepository, never()).delete(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void update_whenStatusBecomesFinished_publishesBookFinishedEvent() {
        Author author = Author.builder().id(1L).build();
        Book existing = Book.builder().id(1L).title("1984").readingStatus(ReadingStatus.READING).build();
        BookDto dto = BookDto.builder().title("1984").authorId(1L).readingStatus(ReadingStatus.FINISHED).build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(existing)).thenReturn(existing);
        when(bookMapper.toDto(existing)).thenReturn(dto);

        bookService.update(1L, dto);

        ArgumentCaptor<BookEvent> captor = ArgumentCaptor.forClass(BookEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(BookEventType.BOOK_FINISHED, captor.getValue().type());
        assertEquals(1L, captor.getValue().bookId());
    }

    @Test
    void update_whenBookWasAlreadyFinished_publishesNothing() {
        Author author = Author.builder().id(1L).build();
        Book existing = Book.builder().id(1L).title("1984").readingStatus(ReadingStatus.FINISHED).build();
        BookDto dto = BookDto.builder().title("1984").authorId(1L).readingStatus(ReadingStatus.FINISHED).build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(existing)).thenReturn(existing);
        when(bookMapper.toDto(existing)).thenReturn(dto);

        bookService.update(1L, dto);

        verifyNoInteractions(eventPublisher);
    }
}