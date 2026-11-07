package com.edu.unicauca.gasstation.backend.sales.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import com.edu.unicauca.gasstation.backend.sales.api.dto.DominusBalanceRequestDto;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService.Evaluation;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashDifferenceStatus;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceJournalNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotEditableException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CashBalanceControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 1);
    private static final String URL = "/api/v1/sales/cash-balance/2026-08-01";

    @Mock
    private CashBalanceStepService service;

    private CashBalanceController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new CashBalanceController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CashBalanceExceptionHandler())
                .build();
    }

    private static Authentication authenticated(String authority) {
        return new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    private static Evaluation shortfall() {
        CashBalanceTableInfo table = new CashBalanceTableInfo(DAY, 288L, "OPEN",
                new SavingsFundInfo(cop(0), cop(250_000), cop(0), cop(250_000)),
                new ExpensesInfo(List.of(), cop(0)), cop(12_114_000), cop(12_516_000), cop(24_630_000), cop(24_629_469),
                cop(-531));
        return new Evaluation(table, true, List.of(), CashDifferenceStatus.PLANILLA_MAYOR);
    }

    @Test
    void getReturnsTheTableTheDifferenceAndTheState() throws Exception {
        when(service.evaluate(DAY)).thenReturn(shortfall());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canComplete").value(true))
                .andExpect(jsonPath("$.differenceStatus").value("PLANILLA_MAYOR"))
                .andExpect(jsonPath("$.table.difference").value(-531))
                .andExpect(jsonPath("$.table.dailyCash").value(12516000))
                .andExpect(jsonPath("$.table.planillaCashBalance").value(24630000))
                .andExpect(jsonPath("$.table.dominusAccumulatedBalance").value(24629469))
                .andExpect(jsonPath("$.missing").isEmpty());
    }

    @Test
    void getOnADateWithoutJournalIs404WithAStableCode() throws Exception {
        when(service.evaluate(DAY)).thenThrow(new CashBalanceJournalNotFoundException(DAY));

        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }

    @Test
    void getWithAnInvalidDateIs400() throws Exception {
        mockMvc.perform(get("/api/v1/sales/cash-balance/not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void putStoresTheDominusFiguresForAnAdmin() throws Exception {
        when(service.saveDominusBalance(DAY, new BigDecimal("24629469")))
                .thenReturn(shortfall());

        mockMvc.perform(put(URL + "/dominus")
                        .principal(authenticated("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accumulatedBalance\":24629469}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.table.difference").value(-531));

        verify(service).saveDominusBalance(DAY, new BigDecimal("24629469"));
    }

    @Test
    void putWithoutTheAdminAuthorityIsRefusedAndStoresNothing() {
        DominusBalanceRequestDto request = new DominusBalanceRequestDto(cop(1));

        assertThrows(AccessDeniedException.class,
                () -> controller.saveDominus(DAY, request, authenticated("READ_ONLY")));
        verify(service, never()).saveDominusBalance(any(), any());
    }

    @Test
    void putWithMissingFiguresIs400AndListsTheInvalidFields() throws Exception {
        mockMvc.perform(put(URL + "/dominus")
                        .principal(authenticated("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("accumulatedBalance"));

        verify(service, never()).saveDominusBalance(any(), any());
    }

    @Test
    void putWithMoreThanTwoDecimalsIs400() throws Exception {
        mockMvc.perform(put(URL + "/dominus")
                        .principal(authenticated("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accumulatedBalance\":1.234}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("accumulatedBalance"));
    }

    @Test
    void putOnAClosedJournalIs409() throws Exception {
        when(service.saveDominusBalance(DAY, new BigDecimal("1")))
                .thenThrow(new CashBalanceNotEditableException("La jornada del 2026-08-01 está cerrada."));

        mockMvc.perform(put(URL + "/dominus")
                        .principal(authenticated("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accumulatedBalance\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CASH_BALANCE_NOT_EDITABLE"));
    }

    @Test
    void putWithABrokenBodyIs400() throws Exception {
        mockMvc.perform(put(URL + "/dominus")
                        .principal(authenticated("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
