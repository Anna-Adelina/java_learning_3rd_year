package com.booktracker.booktracker.validation;

import com.booktracker.booktracker.annotation.ValidReadingProgress;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Бізнес-правила прогресу читання:
 * <ol>
 *   <li>pagesRead не можна вказати без totalPages;</li>
 *   <li>pagesRead не може перевищувати totalPages;</li>
 *   <li>книга зі статусом PLANNED не може мати прочитаних сторінок;</li>
 *   <li>книга зі статусом FINISHED має мати pagesRead == totalPages (якщо вони задані).</li>
 * </ol>
 * Порожні (null) значення вважаються валідними, щоб не ламати існуючі запити.
 * Екземпляр створює сам Hibernate Validator — вручну його ніде не створюємо.
 */
public class ReadingProgressValidator implements ConstraintValidator<ValidReadingProgress, BookDto> {

    @Override
    public boolean isValid(BookDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        Integer total = dto.getTotalPages();
        Integer read = dto.getPagesRead();
        ReadingStatus status = dto.getReadingStatus();

        String error = null;
        if (read != null && total == null) {
            error = "pagesRead cannot be set without totalPages";
        } else if (read != null && total != null && read > total) {
            error = "pagesRead (" + read + ") cannot exceed totalPages (" + total + ")";
        } else if (status == ReadingStatus.PLANNED && read != null && read > 0) {
            error = "A PLANNED book cannot have pages read (pagesRead=" + read
                    + "); change the status to READING or set pagesRead to 0";
        } else if (status == ReadingStatus.FINISHED && read != null && total != null && !read.equals(total)) {
            error = "A FINISHED book must have pagesRead equal to totalPages (read " + read + " of " + total + ")";
        }

        if (error == null) {
            return true;
        }

        // Замість загального message() додаємо конкретне повідомлення, прив'язане до поля pagesRead
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error)
                .addPropertyNode("pagesRead")
                .addConstraintViolation();
        return false;
    }
}
