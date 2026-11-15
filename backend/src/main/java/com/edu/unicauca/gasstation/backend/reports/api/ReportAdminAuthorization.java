package com.edu.unicauca.gasstation.backend.reports.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * Local ADMIN guard of the reports module. It avoids depending on the internal guards of sales.
 *
 * @author javiersolanop777
 */
final class ReportAdminAuthorization {

    private ReportAdminAuthorization() {
    }

    static void requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Se requiere autenticación");
        }
        if (authentication.getAuthorities().stream().noneMatch(authority -> "ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Se requiere la autoridad ADMIN");
        }
    }
}
