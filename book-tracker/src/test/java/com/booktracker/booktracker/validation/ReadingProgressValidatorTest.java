package com.booktracker.booktracker.validation;

import com.booktracker.booktracker.annotation.ValidReadingProgress;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** AC1–AC3: Validator сам знаходить ReadingProgressValidator, без ручного new. */
class ReadingProgressValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private BookDto.BookDtoBuilder base() {
        return BookDto.builder().title("Dune").authorId(1L).readingStatus(ReadingStatus.READING);
    }

    private Set<ConstraintViolation<BookDto>> readingProgressViolations(BookDto dto) {
        return validator.validate(dto).stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation() instanceof ValidReadingProgress)
                .collect(Collectors.toSet());
    }

    // ---- AC2: валідні DTO ----
    @Test
    void validProgress_noViolation() {
        assertTrue(readingProgressViolations(base().totalPages(300).pagesRead(120).build()).isEmpty());
    }

    @Test
    void noProgressAtAll_noViolation() {
        assertTrue(readingProgressViolations(base().build()).isEmpty());
    }

    @Test
    void finishedWithAllPagesRead_noViolation() {
        BookDto dto = base().readingStatus(ReadingStatus.FINISHED).totalPages(300).pagesRead(300).build();
        assertTrue(readingProgressViolations(dto).isEmpty());
    }

    // ---- AC3: порушення з конкретним повідомленням ----
    @Test
    void pagesReadWithoutTotal_violation() {
        var violations = readingProgressViolations(base().pagesRead(10).build());
        assertEquals(1, violations.size());
        assertEquals("pagesRead cannot be set without totalPages", violations.iterator().next().getMessage());
    }

    @Test
    void pagesReadGreaterThanTotal_violationWithNumbers() {
        var violations = readingProgressViolations(base().totalPages(100).pagesRead(150).build());
        assertEquals(1, violations.size());
        var v = violations.iterator().next();
        assertEquals("pagesRead (150) cannot exceed totalPages (100)", v.getMessage());
        assertEquals("pagesRead", v.getPropertyPath().toString());
    }

    @Test
    void plannedWithPagesRead_violation() {
        BookDto dto = base().readingStatus(ReadingStatus.PLANNED).totalPages(100).pagesRead(5).build();
        var violations = readingProgressViolations(dto);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("PLANNED book cannot have pages read"));
    }

    @Test
    void finishedWithUnreadPages_violation() {
        BookDto dto = base().readingStatus(ReadingStatus.FINISHED).totalPages(300).pagesRead(120).build();
        var violations = readingProgressViolations(dto);
        assertEquals(1, violations.size());
        assertEquals("A FINISHED book must have pagesRead equal to totalPages (read 120 of 300)",
                violations.iterator().next().getMessage());
    }
}
