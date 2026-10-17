package com.booktracker.booktracker.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryOnTransientFailureAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private RetryOnTransientFailure retry;

    @InjectMocks
    private RetryOnTransientFailureAspect aspect;

    @Test
    void retry_succeedsOnFirstAttempt_proceedsOnceAndReturnsResult() throws Throwable {
        // Arrange
        when(retry.maxAttempts()).thenReturn(3);
        when(pjp.proceed()).thenReturn("ok");

        // Act
        Object result = aspect.retry(pjp, retry);

        // Assert
        assertEquals("ok", result);
        verify(pjp, times(1)).proceed();
    }

    @Test
    void retry_failsTwiceThenSucceeds_proceedsThreeTimesAndReturnsResult() throws Throwable {
        // Arrange
        when(retry.maxAttempts()).thenReturn(3);
        when(pjp.proceed())
                .thenThrow(new TransientDataAccessResourceException("db hiccup"))
                .thenThrow(new TransientDataAccessResourceException("db hiccup"))
                .thenReturn("ok");

        // Act
        Object result = aspect.retry(pjp, retry);

        // Assert
        assertEquals("ok", result);
        verify(pjp, times(3)).proceed();
    }

    @Test
    void retry_alwaysFailsTransiently_stopsAfterMaxAttemptsAndRethrows() throws Throwable {
        // Arrange
        when(retry.maxAttempts()).thenReturn(3);
        when(pjp.proceed()).thenThrow(new TransientDataAccessResourceException("db down"));

        // Act + Assert
        assertThrows(TransientDataAccessResourceException.class, () -> aspect.retry(pjp, retry));
        verify(pjp, times(3)).proceed();
    }

    @Test
    void retry_nonTransientException_isNotRetried() throws Throwable {
        // Arrange
        when(retry.maxAttempts()).thenReturn(3);
        when(pjp.proceed()).thenThrow(new IllegalStateException("business error"));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> aspect.retry(pjp, retry));
        verify(pjp, times(1)).proceed();
    }
}