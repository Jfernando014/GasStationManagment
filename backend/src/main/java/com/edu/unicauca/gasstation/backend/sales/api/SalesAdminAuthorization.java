package com.edu.unicauca.gasstation.backend.sales.api;

import org.springframework.security.authentication.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/** Local sales write guard; independent of disabled method-security annotations. */
public final class SalesAdminAuthorization {
    private SalesAdminAuthorization() { }
    public static void requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || new AuthenticationTrustResolverImpl().isAnonymous(authentication))
            throw new AuthenticationCredentialsNotFoundException("Se requiere autenticación");
        if (authentication.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority())))
            throw new AccessDeniedException("Se requiere la autoridad ADMIN");
    }
}
