package com.booktracker.booktracker.annotation;

import com.booktracker.booktracker.validation.ReadingProgressValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ТИП 1 — Bean Validation constraint (рівень класу).
 * Перевіряє узгодженість прогресу читання книги: totalPages, pagesRead та readingStatus.
 * Конкретні повідомлення формуються у ReadingProgressValidator
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReadingProgressValidator.class)
public @interface ValidReadingProgress {

    String message() default "Reading progress is inconsistent";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
