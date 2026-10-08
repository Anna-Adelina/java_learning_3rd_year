package com.booktracker.booktracker.dto;

import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.annotation.ValidReadingProgress;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidReadingProgress
public class BookDto {
    private Long id;

    @NotBlank
    private String title;

    @NotNull
    private Long authorId;

    @NotNull
    private ReadingStatus readingStatus;

    @Min(value = 1, message = "totalPages must be at least 1")
    private Integer totalPages;

    @Min(value = 0, message = "pagesRead cannot be negative")
    private Integer pagesRead;

    private LocalDateTime createdAt;

    /** Заповнюється на сервері через @CurrentUsername, значення від клієнта ігнорується. */
    private String createdBy;
}