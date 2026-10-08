package com.booktracker.booktracker.service;

import com.booktracker.booktracker.dto.AuthorDto;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.mapper.AuthorMapper;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.service.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) //підключає Mockito до JUnit 5
class AuthorServiceTest {

    @Mock //створює фальшивий об'єкт Repository
    private AuthorRepository authorRepository;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks //створює реальний об'єкт та підставляє в нього створені моки
    private AuthorService authorService;

    @Test
    void findAll_whenAuthorsExist_returnsMappedDtoList() { //позитивний сценарій
        // Arrange — готуємо дані й "вчимо" моки, що відповідати
        Author author = Author.builder().id(1L).firstName("George").lastName("Orwell").build();
        AuthorDto dto = AuthorDto.builder().id(1L).firstName("George").lastName("Orwell").build();
        when(authorRepository.findAll()).thenReturn(List.of(author));
        when(authorMapper.toDto(author)).thenReturn(dto);

        // Act — викликаємо метод, який тестуємо
        List<AuthorDto> result = authorService.findAll();

        // Assert — перевіряємо результат
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void findById_whenAuthorExists_returnsDto() {
        // Arrange
        Author author = Author.builder().id(1L).firstName("George").lastName("Orwell").build();
        AuthorDto dto = AuthorDto.builder().id(1L).firstName("George").lastName("Orwell").build();
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toDto(author)).thenReturn(dto);

        // Act
        AuthorDto result = authorService.findById(1L);

        // Assert
        assertEquals(dto, result);
    }

    @Test
    void findById_whenAuthorNotFound_throwsEntityNotFoundException() {  //граничні значення: id, якого немає 99L — гранична умова
        // Arrange
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> authorService.findById(99L));
    }

    @Test
    void create_whenCalled_ignoresClientSuppliedIdAndSaves() {
        // Arrange
        AuthorDto inputDto = AuthorDto.builder().id(999L).firstName("George").lastName("Orwell").build();
        Author mappedEntity = Author.builder().id(999L).firstName("George").lastName("Orwell").build();
        Author savedEntity = Author.builder().id(1L).firstName("George").lastName("Orwell").build();
        AuthorDto resultDto = AuthorDto.builder().id(1L).firstName("George").lastName("Orwell").build();

        when(authorMapper.toEntity(inputDto)).thenReturn(mappedEntity);
        when(authorRepository.save(any(Author.class))).thenReturn(savedEntity);
        when(authorMapper.toDto(savedEntity)).thenReturn(resultDto);

        // Act
        AuthorDto result = authorService.create(inputDto);

        // Assert
        ArgumentCaptor<Author> captor = ArgumentCaptor.forClass(Author.class);
        verify(authorRepository).save(captor.capture());
        assertNull(captor.getValue().getId(), "id клієнта має бути обнулений перед збереженням");
        assertEquals(resultDto, result);
    }

    @Test
    void update_whenAuthorExists_updatesFieldsAndSaves() {
        // Arrange
        Author existing = Author.builder().id(1L).firstName("Old").lastName("Name").build();
        AuthorDto updateDto = AuthorDto.builder().firstName("George").lastName("Orwell").build();
        AuthorDto resultDto = AuthorDto.builder().id(1L).firstName("George").lastName("Orwell").build();

        when(authorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(authorRepository.save(existing)).thenReturn(existing);
        when(authorMapper.toDto(existing)).thenReturn(resultDto);

        // Act
        AuthorDto result = authorService.update(1L, updateDto);

        // Assert
        ArgumentCaptor<Author> captor = ArgumentCaptor.forClass(Author.class);
        verify(authorRepository).save(captor.capture());
        assertEquals("George", captor.getValue().getFirstName());
        assertEquals("Orwell", captor.getValue().getLastName());
        assertEquals(resultDto, result);
    }

    @Test
    void update_whenAuthorNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());
        AuthorDto updateDto = AuthorDto.builder().firstName("George").lastName("Orwell").build();

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> authorService.update(99L, updateDto));
    }

    @Test
    void delete_whenAuthorExists_deletesById() { // сервіс перевірить, чи існує автор із ID 1
        // Arrange
        when(authorRepository.existsById(1L)).thenReturn(true);

        // Act
        authorService.delete(1L);

        // Assert
        verify(authorRepository).deleteById(1L);
    }

    @Test
    void delete_whenAuthorNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(authorRepository.existsById(99L)).thenReturn(false);

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> authorService.delete(99L));
        verify(authorRepository, never()).deleteById(anyLong());
    }
}