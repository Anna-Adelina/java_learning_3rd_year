package com.booktracker.booktracker.aop;

import com.booktracker.booktracker.service.exception.TooManyRequestsException;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitedAspectTest {

    @Mock
    private Clock clock;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private RateLimited limit;

    private RateLimitedAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitedAspect(clock);
        when(limit.maxCalls()).thenReturn(2);
        when(limit.perSeconds()).thenReturn(60);
        when(joinPoint.toShortString()).thenReturn("AuthorService.create(..)");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String name) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(name, null, "ROLE_USER"));
    }

    @Test
    void enforce_callsWithinLimit_areAllowed() {
        // Arrange
        when(clock.millis()).thenReturn(1_000L);
        loginAs("anna");

        // Act + Assert
        assertDoesNotThrow(() -> {
            aspect.enforce(joinPoint, limit);
            aspect.enforce(joinPoint, limit);
        });
    }

    @Test
    void enforce_callAboveLimit_throwsTooManyRequestsWithClearMessage() {
        // Arrange
        when(clock.millis()).thenReturn(1_000L);
        loginAs("anna");
        aspect.enforce(joinPoint, limit);
        aspect.enforce(joinPoint, limit);

        // Act
        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> aspect.enforce(joinPoint, limit));

        // Assert
        assertTrue(ex.getMessage().contains("at most 2 calls per 60 s"));
    }

    @Test
    void enforce_afterWindowExpires_allowsCallsAgain() {
        // Arrange: два виклики в момент 1с, третій через 61с
        when(clock.millis()).thenReturn(1_000L, 1_000L, 62_000L);
        loginAs("anna");
        aspect.enforce(joinPoint, limit);
        aspect.enforce(joinPoint, limit);

        // Act + Assert
        assertDoesNotThrow(() -> aspect.enforce(joinPoint, limit));
    }

    @Test
    void enforce_limitIsTrackedPerUser() {
        // Arrange
        when(clock.millis()).thenReturn(1_000L);
        loginAs("anna");
        aspect.enforce(joinPoint, limit);
        aspect.enforce(joinPoint, limit);

        // Act + Assert: інший користувач має власний ліміт
        loginAs("ivan");
        assertDoesNotThrow(() -> aspect.enforce(joinPoint, limit));
    }
}