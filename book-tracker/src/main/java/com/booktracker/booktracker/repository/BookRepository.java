package com.booktracker.booktracker.repository;

import com.booktracker.booktracker.entity.Book;
import com.booktracker.booktracker.entity.ReadingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
        select b from Book b
        where b.readingStatus = :status
          and b.createdBy is not null
          and (b.lastRemindedAt is null or b.lastRemindedAt < :threshold)
        order by b.id
        """)
    List<Book> findDueForReminder(@Param("status") ReadingStatus status,
                                  @Param("threshold") LocalDateTime threshold,
                                  Pageable pageable);

    long countByReadingStatus(ReadingStatus status);
}