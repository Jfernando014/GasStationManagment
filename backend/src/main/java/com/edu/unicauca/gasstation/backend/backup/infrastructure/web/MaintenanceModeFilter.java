package com.edu.unicauca.gasstation.backend.backup.infrastructure.web;

import com.edu.unicauca.gasstation.backend.backup.application.MaintenanceMode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers 503 to normal requests while a restore keeps the database under maintenance.
 *
 * @author javiersolanop777
 */
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final MaintenanceMode atrMaintenanceMode;

    public MaintenanceModeFilter(MaintenanceMode prmMaintenanceMode) {
        this.atrMaintenanceMode = prmMaintenanceMode;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest prmRequest,
            @NonNull HttpServletResponse prmResponse, @NonNull FilterChain prmChain)
            throws ServletException, IOException {
        String objPath = prmRequest.getRequestURI();
        boolean objAllowed = objPath.startsWith("/api/v1/backups")
                || objPath.equals("/error");
        if (!atrMaintenanceMode.isActive() || objAllowed) {
            prmChain.doFilter(prmRequest, prmResponse);
            return;
        }

        prmResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        prmResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        prmResponse.getWriter().write("""
                {"type":"about:blank","title":"Service Unavailable","status":503,\
                "detail":"The database is being restored. Try again in a moment.",\
                "code":"BACKUP_RESTORE_IN_PROGRESS"}
                """);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}
