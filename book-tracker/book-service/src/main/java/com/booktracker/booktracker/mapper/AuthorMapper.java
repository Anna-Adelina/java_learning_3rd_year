package com.booktracker.booktracker.mapper;

import com.booktracker.booktracker.dto.AuthorDto;
import com.booktracker.booktracker.entity.Author;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthorMapper {

    AuthorDto toDto(Author author);

    Author toEntity(AuthorDto dto);
}