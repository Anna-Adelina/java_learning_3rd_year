package com.booktracker.booktracker.dto;

public record BookDetailsDto(Long bookId, String title, Double rating, String authorBio, boolean partial) {
}