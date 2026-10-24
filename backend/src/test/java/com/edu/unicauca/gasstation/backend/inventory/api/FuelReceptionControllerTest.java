package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelReceptionRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelReceptionService;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelReceptionNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidFuelReceptionException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ReceptionExceedsCapacityException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelReceptionMapperImpl;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reception;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FuelReceptionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(FuelReceptionMapperImpl.class)
class FuelReceptionControllerTest {

    private static final String URL = "/api/v1/inventory/receptions";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FuelReceptionService receptionService;


    // ---------- POST ----------

    @Test
    void createReturnsCreatedWithTheReception() throws Exception {
        when(receptionService.create(any(FuelReception.class))).thenReturn(reception(1L, 1L, DATE, "3970"));

        post(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tankId").value(1))
                .andExpect(jsonPath("$.receptionDate").value("2026-08-05"))
                .andExpect(jsonPath("$.gallons").value(3970))
                .andExpect(jsonPath("$.supplier").value("Terpel"));
    }

    @Test
    void createSendsTheRequestDataToTheService() throws Exception {
        when(receptionService.create(any(FuelReception.class))).thenReturn(reception(1L, 1L, DATE, "3970"));

        post(validRequest()).andExpect(status().isCreated());

        verify(receptionService).create(argThat(r -> r.getId() == null
                && r.getTankId().equals(1L)
                && r.getReceptionDate().equals(DATE)
                && r.getGallons().compareTo(new BigDecimal("3970")) == 0
                && r.getSupplier().equals("Terpel")));
    }

    @Test
    void createRejectsAZeroOrNegativeQuantity() throws Exception {
        for (String gallons : List.of("0", "-5")) {
            FuelReceptionRequest request = validRequest();
            request.setGallons(new BigDecimal(gallons));

            post(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].field").value("gallons"))
                    .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.GALLONS_POSITIVE));
        }
        verifyNoInteractions(receptionService);
    }

    @Test
    void createRejectsAMissingQuantity() throws Exception {
        FuelReceptionRequest request = validRequest();
        request.setGallons(null);

        post(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.GALLONS_REQUIRED));
        verifyNoInteractions(receptionService);
    }

    @Test
    void createRejectsAQuantityWithTooManyDigits() throws Exception {
        for (String gallons : List.of("1234567", "10.123")) {
            FuelReceptionRequest request = validRequest();
            request.setGallons(new BigDecimal(gallons));

            post(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.GALLONS_DIGITS));
        }
        verifyNoInteractions(receptionService);
    }

    @Test
    void createRejectsAQuantityThatIsNotANumber() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tankId\":1,\"receptionDate\":\"2026-08-05\",\"gallons\":\"abc\",\"supplier\":\"Terpel\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"));
        verifyNoInteractions(receptionService);
    }

    @Test
    void createRejectsAMissingDate() throws Exception {
        FuelReceptionRequest request = validRequest();
        request.setReceptionDate(null);

        post(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("receptionDate"))
                .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.DATE_INVALID));
        verifyNoInteractions(receptionService);
    }

    @Test
    void createRejectsAMissingTank() throws Exception {
        FuelReceptionRequest request = validRequest();
        request.setTankId(null);

        post(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.TANK_REQUIRED));
        verifyNoInteractions(receptionService);
    }

    @Test
    void createRejectsABlankOrTooLongSupplier() throws Exception {
        FuelReceptionRequest blank = validRequest();
        blank.setSupplier("   ");
        post(blank)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.SUPPLIER_REQUIRED));

        FuelReceptionRequest tooLong = validRequest();
        tooLong.setSupplier("x".repeat(101));
        post(tooLong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(FuelReceptionRequest.SUPPLIER_SIZE));
        verifyNoInteractions(receptionService);
    }

    @Test
    void createReturnsConflictWhenTheQuantityExceedsTheCapacity() throws Exception {
        when(receptionService.create(any(FuelReception.class))).thenThrow(new ReceptionExceedsCapacityException());

        post(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(new ReceptionExceedsCapacityException().getMessage()));
    }

    @Test
    void createReturnsBadRequestWhenTheDateIsInTheFuture() throws Exception {
        when(receptionService.create(any(FuelReception.class)))
                .thenThrow(new InvalidFuelReceptionException(FuelReceptionRequest.DATE_INVALID));

        post(validRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value(FuelReceptionRequest.DATE_INVALID));
    }

    @Test
    void createReturnsNotFoundForAnUnknownTank() throws Exception {
        when(receptionService.create(any(FuelReception.class))).thenThrow(new TankNotFoundException(99L));

        post(validRequest()).andExpect(status().isNotFound());
    }

    // ---------- PUT ----------

    @Test
    void updateReturnsTheUpdatedReception() throws Exception {
        when(receptionService.update(eq(7L), any(FuelReception.class))).thenReturn(reception(7L, 1L, DATE, "3970"));

        putReception(7L, validRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void updateReturnsNotFoundWhenTheReceptionDoesNotExist() throws Exception {
        when(receptionService.update(eq(99L), any(FuelReception.class))).thenThrow(new FuelReceptionNotFoundException(99L));

        putReception(99L, validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value(new FuelReceptionNotFoundException(99L).getMessage()));
    }

    @Test
    void updateReturnsConflictWhenTheQuantityExceedsTheCapacity() throws Exception {
        when(receptionService.update(eq(7L), any(FuelReception.class))).thenThrow(new ReceptionExceedsCapacityException());

        putReception(7L, validRequest()).andExpect(status().isConflict());
    }

    @Test
    void updateRejectsAnInvalidBody() throws Exception {
        FuelReceptionRequest request = validRequest();
        request.setGallons(BigDecimal.ZERO);

        putReception(7L, request).andExpect(status().isBadRequest());
        verifyNoInteractions(receptionService);
    }

    // ---------- GET ----------

    @Test
    void findByIdReturnsTheReception() throws Exception {
        when(receptionService.findById(7L)).thenReturn(reception(7L, 1L, DATE, "3970"));

        mockMvc.perform(get(URL + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.supplier").value("Terpel"));
    }

    @Test
    void findByIdReturnsNotFoundWhenTheReceptionDoesNotExist() throws Exception {
        when(receptionService.findById(99L)).thenThrow(new FuelReceptionNotFoundException(99L));

        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound());
    }

    @Test
    void searchPassesTheFiltersToTheService() throws Exception {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        when(receptionService.search(1L, from, to)).thenReturn(List.of(
                reception(2L, 1L, DATE, "200"), reception(1L, 1L, from, "100")));

        mockMvc.perform(get(URL).param("tankId", "1").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void searchWithoutFiltersReturnsAnEmptyListWhenThereAreNoReceptions() throws Exception {
        when(receptionService.search(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchReturnsBadRequestWhenTheStartDateIsAfterTheEndDate() throws Exception {
        when(receptionService.search(null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)))
                .thenThrow(new InvalidFuelReceptionException("The start date cannot be after the end date"));

        mockMvc.perform(get(URL).param("from", "2026-09-01").param("to", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("The start date cannot be after the end date"));
    }

    @Test
    void searchRejectsAnInvalidDate() throws Exception {
        mockMvc.perform(get(URL).param("from", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'from' is invalid"));
        verifyNoInteractions(receptionService);
    }

    // ---------- helpers ----------

    private FuelReceptionRequest validRequest() {
        FuelReceptionRequest request = new FuelReceptionRequest();
        request.setTankId(1L);
        request.setReceptionDate(DATE);
        request.setGallons(new BigDecimal("3970"));
        request.setSupplier("Terpel");
        return request;
    }

    private ResultActions post(FuelReceptionRequest request) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putReception(Long id, FuelReceptionRequest request) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}