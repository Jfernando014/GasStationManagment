package com.edu.unicauca.gasstation.backend.competition.api;

import org.springframework.security.authentication.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/** Matches the project's explicit ADMIN guard; method security is not enabled globally. */
final class CompetitionAdminAuthorization {
    private CompetitionAdminAuthorization() { }
    static void requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication))
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        if (authentication.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority())))
            throw new AccessDeniedException("ADMIN authority required");
    }
}
