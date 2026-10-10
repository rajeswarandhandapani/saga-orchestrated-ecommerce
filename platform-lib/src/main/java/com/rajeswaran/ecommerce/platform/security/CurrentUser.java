package com.rajeswaran.ecommerce.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identity of the caller, taken from the validated JWT - never from request bodies. */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Keycloak user id ({@code sub}); used as CustomerId in the Ordering and Payments contexts. */
    public static String id() {
        return jwt().getSubject();
    }

    public static String username() {
        return jwt().getClaimAsString("preferred_username");
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public static String bearerToken() {
        return jwt().getTokenValue();
    }

    private static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("No JWT-authenticated user in context");
    }
}
