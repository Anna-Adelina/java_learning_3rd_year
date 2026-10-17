package com.booktracker.booktracker.aop;

import com.booktracker.booktracker.dto.BookDto;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditedAspectTest {

    private static final Instant NOW = Instant.parse("2026-10-17T10:00:00Z");

    @Mock
    private AuditTrail auditTrail;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private Audited audited;

    private AuditedAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AuditedAspect(auditTrail, Clock.fixed(NOW, ZoneOffset.UTC));
        when(audited.action()).thenReturn("BOOK_CREATED");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("BookService.create(..)");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String name) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(name, null, "ROLE_USER"));
    }

    private AuditEntry recordedEntry() {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditTrail).record(captor.capture());
        return captor.getValue();
    }

    @Test
    void audit_resultHasId_recordsEntryWithIdFromResult() {
        // Arrange
        loginAs("anna");
        BookDto created = BookDto.builder().id(7L).title("1984").build();

        // Act
        aspect.audit(joinPoint, audited, created);

        // Assert
        AuditEntry entry = recordedEntry();
        assertEquals("anna", entry.user());
        assertEquals("BOOK_CREATED", entry.action());
        assertEquals("BookService.create(..)", entry.method());
        assertEquals("7", entry.resourceId());
        assertEquals(NOW, entry.timestamp());
    }

    @Test
    void audit_voidMethodWithIdArgument_recordsIdFromArgument() {
        // Arrange
        loginAs("anna");
        when(joinPoint.getArgs()).thenReturn(new Object[]{5L});

        // Act
        aspect.audit(joinPoint, audited, null);

        // Assert
        assertEquals("5", recordedEntry().resourceId());
    }

    @Test
    void audit_noIdAnywhere_recordsEntryWithoutResourceId() {
        // Arrange
        loginAs("anna");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"text"});

        // Act
        aspect.audit(joinPoint, audited, null);

        // Assert
        assertNull(recordedEntry().resourceId());
    }

    @Test
    void audit_withoutAuthenticatedUser_recordsAnonymous() {
        // Arrange
        BookDto created = BookDto.builder().id(7L).build();

        // Act
        aspect.audit(joinPoint, audited, created);

        // Assert
        assertEquals("anonymous", recordedEntry().user());
    }
}