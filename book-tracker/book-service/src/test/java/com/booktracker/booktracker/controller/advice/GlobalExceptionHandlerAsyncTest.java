package com.booktracker.booktracker.controller.advice;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerAsyncTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTaskRejected_returns503WithRetryAfterAndClearMessage() {
        // Act
        ResponseEntity<Map<String, Object>> response =
                handler.handleTaskRejected(new TaskRejectedException("pool is full"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("5", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals(503, response.getBody().get("status"));
        assertTrue(String.valueOf(response.getBody().get("message")).contains("try again later"));
    }
}