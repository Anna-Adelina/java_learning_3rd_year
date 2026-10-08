package com.booktracker.booktracker.mapper;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(source = "author.id", target = "authorId")
    BookDto toDto(Book book);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Book toEntity(BookDto dto);
}