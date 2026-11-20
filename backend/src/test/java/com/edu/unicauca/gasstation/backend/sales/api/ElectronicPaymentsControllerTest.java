package com.edu.unicauca.gasstation.backend.sales.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.application.ElectronicPaymentsService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** The reset of step 3: ADMIN only, and the conflicts of the journal come out as 409. */
@ExtendWith(MockitoExtension.class)
class ElectronicPaymentsControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 5);
    private static final String URL = "/api/v1/sales/electronic-payments/2026-08-05";

    @Mock
    private ElectronicPaymentsService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ElectronicPaymentsController(service))
                .setControllerAdvice(new CardReconciliationExceptionHandler())
                .build();
    }

    private static Authentication authenticated(String authority) {
        return new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }

    @Test
    void deleteResetsStepThreeForAnAdminAndAnswersNoContent() throws Exception {
        mockMvc.perform(delete(URL).principal(authenticated("ADMIN")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).reset(DAY);
    }

    @Test
    void deleteWithoutTheAdminAuthorityIs403AndResetsNothing() throws Exception {
        mockMvc.perform(delete(URL).principal(authenticated("READ_ONLY")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(service, never()).reset(any());
    }

    @Test
    void deleteWithoutAuthenticationIs401AndResetsNothing() throws Exception {
        mockMvc.perform(delete(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verify(service, never()).reset(any());
    }

    @Test
    void deleteOnAClosedJournalIs409() throws Exception {
        doThrow(new IllegalStateException("La jornada del 2026-08-05 ya está cerrada y no permite modificaciones."))
                .when(service).reset(DAY);

        mockMvc.perform(delete(URL).principal(authenticated("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOURNAL_STATE_CONFLICT"))
                .andExpect(jsonPath("$.detail").value(
                        "La jornada del 2026-08-05 ya está cerrada y no permite modificaciones."));
    }

    @Test
    void deleteWithAnInvalidDateIs400AndResetsNothing() throws Exception {
        mockMvc.perform(delete("/api/v1/sales/electronic-payments/not-a-date").principal(authenticated("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        verify(service, never()).reset(any());
    }
}
