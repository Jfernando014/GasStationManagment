package com.edu.unicauca.gasstation.backend.incentives.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/** ADMIN guard for write endpoints; method security is not enabled globally. */
final class IncentiveAdminAuthorization {

    private IncentiveAdminAuthorization() {
    }

    static void requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Se requiere autenticación");
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Se requiere la autoridad ADMIN");
        }
    }
}
