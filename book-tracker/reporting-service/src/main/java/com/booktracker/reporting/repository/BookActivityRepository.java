package com.booktracker.reporting.repository;

import com.booktracker.reporting.entity.BookActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookActivityRepository extends JpaRepository<BookActivity, Long> {

    List<BookActivity> findAllByOrderByIdDesc();

    boolean existsByEventId(String eventId);
}