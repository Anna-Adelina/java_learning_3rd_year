package com.booktracker.booktracker.config;

import com.booktracker.booktracker.annotation.CurrentUsername;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;

/**
 * Дістає ім'я користувача з контексту запиту
 */
public class CurrentUsernameArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PREFERRED_USERNAME = "preferred_username";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUsername.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Principal principal = webRequest.getUserPrincipal();
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user in the request");
        }

        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString(PREFERRED_USERNAME);
            return username != null ? username : jwtAuth.getName();
        }
        if (principal instanceof OAuth2AuthenticationToken oauthToken
                && oauthToken.getPrincipal() instanceof OidcUser oidcUser) {
            String username = oidcUser.getPreferredUsername();
            return username != null ? username : oauthToken.getName();
        }
        return principal.getName();
    }
}
