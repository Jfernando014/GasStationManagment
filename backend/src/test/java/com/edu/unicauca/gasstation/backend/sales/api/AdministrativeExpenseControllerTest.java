package com.edu.unicauca.gasstation.backend.sales.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.api.dto.AdministrativeExpenseRequestDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdministrativeExpenseControllerTest {

    private final List<AdministrativeExpense> expenses = new ArrayList<>();
    private MockMvc mockMvc;
    private AdministrativeExpenseController controller;

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong();
        AdministrativeExpenseRepository repository = new AdministrativeExpenseRepository() {
            @Override
            public AdministrativeExpense save(AdministrativeExpense expense) {
                AdministrativeExpense saved = new AdministrativeExpense(ids.incrementAndGet(), expense.date(),
                        expense.amount(), expense.description(), Instant.now());
                expenses.add(saved);
                return saved;
            }

            @Override
            public List<AdministrativeExpense> findAll() {
                return List.copyOf(expenses);
            }

            @Override
            public List<AdministrativeExpense> findBetween(LocalDate fromDate, LocalDate toDate) {
                return expenses.stream()
                        .filter(expense -> !expense.date().isBefore(fromDate) && !expense.date().isAfter(toDate))
                        .toList();
            }

            @Override
            public Optional<AdministrativeExpense> findById(Long id) {
                return expenses.stream().filter(expense -> expense.id().equals(id)).findFirst();
            }

            @Override
            public void deleteById(Long id) {
                expenses.removeIf(expense -> expense.id().equals(id));
            }
        };
        AdministrativeExpenseService service = new AdministrativeExpenseService(repository);
        controller = new AdministrativeExpenseController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AdministrativeExpenseExceptionHandler())
                .build();
    }

    @Test
    void firstAugustFortnightReturnsTheExpectedTotal() throws Exception {
        register("2026-08-01", "80000.00", "Servicios de energía");
        register("2026-08-10", "25000.00", "Limpieza y mantenimiento");
        register("2026-08-14", "31100.00", "Papelería y suministros");
        register("2026-08-16", "99999.00", "Quincena siguiente");

        mockMvc.perform(get("/api/v1/sales/administrative-expenses")
                        .param("year", "2026").param("month", "8").param("fortnight", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses.length()").value(3))
                .andExpect(jsonPath("$.total").value(136100.0));
    }

    @Test
    void createsExpenseAndRejectsInvalidFortnightWithProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/sales/administrative-expenses")
                        .principal(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-08-01\",\"amount\":15000,\"description\":\"Papelería\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2026-08-01"));

        mockMvc.perform(get("/api/v1/sales/administrative-expenses")
                        .param("year", "2026").param("month", "8").param("fortnight", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ADMINISTRATIVE_EXPENSE"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
        assertEquals(1, expenses.size());
    }

    @Test
    void deletesExpenseThroughTheApi() throws Exception {
        register("2026-08-01", "15000.00", "Papelería");

        mockMvc.perform(delete("/api/v1/sales/administrative-expenses/1").principal(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/sales/administrative-expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    private void register(String date, String amount, String description) throws Exception {
        mockMvc.perform(post("/api/v1/sales/administrative-expenses")
                        .principal(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\",\"amount\":%s,\"description\":\"%s\"}"
                                .formatted(date, amount, description)))
                .andExpect(status().isCreated());
    }

    @Test
    void writeOperationsRejectNonAdminAuthority() {
        Authentication readOnly = new UsernamePasswordAuthenticationToken(
                "reader", "unused", List.of(new SimpleGrantedAuthority("READ_ONLY")));
        AdministrativeExpenseRequestDto request = new AdministrativeExpenseRequestDto(
                LocalDate.of(2026, 8, 1), new BigDecimal("100.00"), "Papelería");

        assertThrows(AccessDeniedException.class, () -> controller.register(request, readOnly));
        assertThrows(AccessDeniedException.class, () -> controller.delete(1L, readOnly));
        assertEquals(0, expenses.size());
    }

    private Authentication admin() {
        return new UsernamePasswordAuthenticationToken(
                "admin", "unused", List.of(new SimpleGrantedAuthority("ADMIN")));
    }
}