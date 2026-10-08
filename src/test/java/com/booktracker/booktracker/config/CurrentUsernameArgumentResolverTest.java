package com.booktracker.booktracker.config;

import com.booktracker.booktracker.annotation.CurrentUsername;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** AC6 (supportsParameter) та AC7 (resolveArgument). */
class CurrentUsernameArgumentResolverTest {

    private final CurrentUsernameArgumentResolver resolver = new CurrentUsernameArgumentResolver();

    @SuppressWarnings("unused")
    static class Handler {
        void annotated(@CurrentUsername String username) {}
        void notAnnotated(String username) {}
        void annotatedWrongType(@CurrentUsername Long id) {}
    }

    private MethodParameter param(String method, Class<?> type) throws NoSuchMethodException {
        Method m = Handler.class.getDeclaredMethod(method, type);
        return new MethodParameter(m, 0);
    }

    private NativeWebRequest requestWith(Principal principal) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(principal);
        return new ServletWebRequest(request);
    }

    // ---- AC6 ----
    @Test
    void supportsParameter_trueForAnnotatedString() throws Exception {
        assertTrue(resolver.supportsParameter(param("annotated", String.class)));
    }

    @Test
    void supportsParameter_falseWithoutAnnotation() throws Exception {
        assertFalse(resolver.supportsParameter(param("notAnnotated", String.class)));
    }

    @Test
    void supportsParameter_falseForWrongType() throws Exception {
        assertFalse(resolver.supportsParameter(param("annotatedWrongType", Long.class)));
    }

    // ---- AC7 ----
    @Test
    void resolveArgument_fromJwt_usesPreferredUsername() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                .subject("uuid-123").claim("preferred_username", "anna").build();
        var auth = new JwtAuthenticationToken(jwt, List.of());

        Object result = resolver.resolveArgument(param("annotated", String.class), null, requestWith(auth), null);

        assertEquals("anna", result);
        assertInstanceOf(String.class, result);
    }

    @Test
    void resolveArgument_fromJwtWithoutClaim_fallsBackToName() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("uuid-123").build();
        var auth = new JwtAuthenticationToken(jwt, List.of());

        Object result = resolver.resolveArgument(param("annotated", String.class), null, requestWith(auth), null);

        assertEquals("uuid-123", result);
    }

    @Test
    void resolveArgument_fromOidcLogin_usesPreferredUsername() throws Exception {
        OidcIdToken idToken = OidcIdToken.withTokenValue("id")
                .subject("uuid-1").issuer("http://localhost:9090/realms/book-tracker")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("preferred_username", "olena").build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        var auth = new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");

        Object result = resolver.resolveArgument(param("annotated", String.class), null, requestWith(auth), null);

        assertEquals("olena", result);
    }

    @Test
    void resolveArgument_plainPrincipal_usesName() throws Exception {
        Object result = resolver.resolveArgument(param("annotated", String.class), null,
                requestWith(new TestingAuthenticationToken("bob", null)), null);
        assertEquals("bob", result);
    }

    @Test
    void resolveArgument_withoutPrincipal_throws() throws Exception {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> resolver.resolveArgument(param("annotated", String.class), null, requestWith(null), null));
    }
}
