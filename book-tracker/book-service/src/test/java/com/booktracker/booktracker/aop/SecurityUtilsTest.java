package com.booktracker.booktracker.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityUtilsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginWithJwt(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    void currentUsername_jwtWithPreferredUsername_returnsPreferredUsername() {
        // Arrange
        loginWithJwt(Jwt.withTokenValue("t").header("alg", "none")
                .subject("uuid-1").claim("preferred_username", "anna").build());

        // Act + Assert
        assertEquals("anna", SecurityUtils.currentUsername());
    }

    @Test
    void currentUsername_jwtWithoutPreferredUsername_fallsBackToSubject() {
        // Arrange
        loginWithJwt(Jwt.withTokenValue("t").header("alg", "none").subject("uuid-1").build());

        // Act + Assert
        assertEquals("uuid-1", SecurityUtils.currentUsername());
    }

    @Test
    void currentUsername_noAuthentication_returnsAnonymous() {
        // Act + Assert
        assertEquals("anonymous", SecurityUtils.currentUsername());
    }
}