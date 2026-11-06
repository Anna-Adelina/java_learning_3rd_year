package com.booktracker.booktracker.aop;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String preferred = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (preferred != null) {
                return preferred;
            }
        }
        if (auth.getPrincipal() instanceof OidcUser oidcUser && oidcUser.getPreferredUsername() != null) {
            return oidcUser.getPreferredUsername();
        }
        return auth.getName();
    }
}