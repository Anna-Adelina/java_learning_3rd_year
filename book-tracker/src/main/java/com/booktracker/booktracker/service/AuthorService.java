package com.booktracker.booktracker.service;

import com.booktracker.booktracker.dto.AuthorDto;
import com.booktracker.booktracker.entity.Author;
import com.booktracker.booktracker.mapper.AuthorMapper;
import com.booktracker.booktracker.repository.AuthorRepository;
import com.booktracker.booktracker.service.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    public List<AuthorDto> findAll() {
        return authorRepository.findAll()
                .stream()
                .map(authorMapper::toDto)
                .toList();
    }

    public AuthorDto findById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Author not found: id=" + id));
        return authorMapper.toDto(author);
    }

    public AuthorDto create(AuthorDto dto) {
        Author author = authorMapper.toEntity(dto);
        author.setId(null); // про це нижче
        Author saved = authorRepository.save(author);
        return authorMapper.toDto(saved);
    }

    public AuthorDto update(Long id, AuthorDto dto) {
        Author existing = authorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Author not found: id=" + id));
        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        Author saved = authorRepository.save(existing);
        return authorMapper.toDto(saved);
    }

    public void delete(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new EntityNotFoundException("Author not found: id=" + id);
        }
        authorRepository.deleteById(id);
    }
}