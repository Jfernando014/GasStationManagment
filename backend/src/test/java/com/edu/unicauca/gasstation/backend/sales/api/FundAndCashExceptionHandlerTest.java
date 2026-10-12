package com.edu.unicauca.gasstation.backend.sales.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateContributionException;
import com.edu.unicauca.gasstation.backend.sales.exception.InsufficientFundException;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidCashMovementException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FundAndCashExceptionHandlerTest {

    private FundAndCashExceptionHandler handler;
    private SavingsFundService savingsFundService;
    private CashService cashService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = new FundAndCashExceptionHandler();
        savingsFundService = mock(SavingsFundService.class);
        cashService = mock(CashService.class);

        SavingsFundController savingsController = new SavingsFundController(savingsFundService);
        CashController cashController = new CashController(cashService);

        mockMvc = MockMvcBuilders.standaloneSetup(savingsController, cashController)
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void duplicateContributionReturns409WithStandardCodeAndTitle() {
        DuplicateContributionException ex = new DuplicateContributionException(LocalDate.of(2026, 8, 1));
        ProblemDetail problem = handler.handleDuplicateContribution(ex);

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Conflict", problem.getTitle());
        assertEquals("DUPLICATE_CONTRIBUTION", problem.getProperties().get("code"));
        assertEquals("Ya existe un aporte registrado para la fecha 2026-08-01", problem.getDetail());
    }

    @Test
    void insufficientFundReturns409WithStandardCodeAndTitle() {
        InsufficientFundException ex = new InsufficientFundException(
                BigDecimal.valueOf(100), BigDecimal.valueOf(500), LocalDate.of(2026, 8, 1));
        ProblemDetail problem = handler.handleInsufficientFund(ex);

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Conflict", problem.getTitle());
        assertEquals("INSUFFICIENT_FUNDS", problem.getProperties().get("code"));
        assertNotNull(problem.getDetail());
    }

    @Test
    void invalidCashMovementReturns400WithStandardCodeAndTitle() {
        InvalidCashMovementException ex = new InvalidCashMovementException("El valor del movimiento no puede ser cero");
        ProblemDetail problem = handler.handleInvalidCashMovement(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("INVALID_CASH_MOVEMENT", problem.getProperties().get("code"));
        assertEquals("El valor del movimiento no puede ser cero", problem.getDetail());
    }

    @Test
    void validationErrorReturns400WithValidationErrorAndFields() throws Exception {
        mockMvc.perform(post("/api/v1/sales/savings-fund/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":null,\"amount\":-100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("La solicitud contiene datos no válidos"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void malformedJsonReturns400WithMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/sales/savings-fund/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud no es válido"));
    }

    @Test
    void typeMismatchReturns400WithInvalidParameter() throws Exception {
        mockMvc.perform(get("/api/v1/sales/savings-fund/balance")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void duplicateContributionViaEndpointReturns409() throws Exception {
        when(savingsFundService.registerContribution(any(), any(), any()))
                .thenThrow(new DuplicateContributionException(LocalDate.of(2026, 8, 1)));

        mockMvc.perform(post("/api/v1/sales/savings-fund/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-08-01\",\"amount\":250000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CONTRIBUTION"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Ya existe un aporte registrado para la fecha 2026-08-01"));
    }
}
