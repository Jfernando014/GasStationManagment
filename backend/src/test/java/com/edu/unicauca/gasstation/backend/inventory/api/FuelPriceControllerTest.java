package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.FuelPriceTestData;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPriceRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelPriceService;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelPriceNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelPriceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FuelPriceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FuelPriceMapper.class)
class FuelPriceControllerTest {

    private static final String BASE_URL = "/api/inventory/fuel-prices";
    private static final LocalDate AUGUST_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUGUST_5 = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FuelPriceService fuelPriceService;

    @Test
    void createFuelPriceReturnsCreatedWithThePrice() throws Exception {
        when(fuelPriceService.create(any(FuelPrice.class)))
                .thenReturn(FuelPriceTestData.price(1L, FuelType.MOTOR, "16330", AUGUST_1));

        postPrice(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fuelType").value("MOTOR"))
                .andExpect(jsonPath("$.validFrom").value("2026-08-01"));
    }

    @Test
    void createFuelPriceRejectsNonPositivePrice() throws Exception {
        FuelPriceRequest request = validRequest();
        request.setPricePerGallon(BigDecimal.ZERO);

        postPrice(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("pricePerGallon"))
                .andExpect(jsonPath("$.fields[0].message").value("El precio debe ser un valor numérico positivo"));
        verifyNoInteractions(fuelPriceService);
    }

    @Test
    void createFuelPriceRejectsMissingValidFrom() throws Exception {
        FuelPriceRequest request = validRequest();
        request.setValidFrom(null);

        postPrice(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("validFrom"))
                .andExpect(jsonPath("$.fields[0].message").value("La fecha de vigencia es obligatoria"));
        verifyNoInteractions(fuelPriceService);
    }

    @Test
    void createFuelPriceReturnsConflictWhenTheDateIsTaken() throws Exception {
        when(fuelPriceService.create(any(FuelPrice.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        postPrice(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto"));
    }

    @Test
    void updateFuelPriceReturnsTheUpdatedPrice() throws Exception {
        when(fuelPriceService.update(eq(1L), any(FuelPrice.class)))
                .thenReturn(FuelPriceTestData.price(1L, FuelType.MOTOR, "17000", AUGUST_1));
        FuelPriceRequest request = validRequest();
        request.setPricePerGallon(new BigDecimal("17000"));

        putPrice(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fuelType").value("MOTOR"));
    }

    @Test
    void updateFuelPriceReturnsNotFoundWhenItDoesNotExist() throws Exception {
        when(fuelPriceService.update(eq(99L), any(FuelPrice.class)))
                .thenThrow(new FuelPriceNotFoundException(99L));

        putPrice(99L, validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe un precio con identificador 99"));
    }

    @Test
    void findEffectiveReturnsThePriceInForce() throws Exception {
        when(fuelPriceService.findEffective(FuelType.MOTOR, AUGUST_5))
                .thenReturn(FuelPriceTestData.price(1L, FuelType.MOTOR, "16330", AUGUST_1));

        mockMvc.perform(get(BASE_URL + "/effective?fuelType=MOTOR&date=2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuelType").value("MOTOR"))
                .andExpect(jsonPath("$.validFrom").value("2026-08-01"));
    }

    @Test
    void findEffectiveReturnsNotFoundWhenNoPriceHasStarted() throws Exception {
        when(fuelPriceService.findEffective(FuelType.MOTOR, AUGUST_5))
                .thenThrow(new FuelPriceNotFoundException(FuelType.MOTOR, AUGUST_5));

        mockMvc.perform(get(BASE_URL + "/effective?fuelType=MOTOR&date=2026-08-05"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay un precio vigente de MOTOR para la fecha 2026-08-05"));
    }

    @Test
    void findEffectiveRejectsUnknownFuelType() throws Exception {
        mockMvc.perform(get(BASE_URL + "/effective?fuelType=GAS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos inválidos"));
        verifyNoInteractions(fuelPriceService);
    }

    private ResultActions postPrice(FuelPriceRequest request) throws Exception {
        return mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putPrice(Long id, FuelPriceRequest request) throws Exception {
        return mockMvc.perform(put(BASE_URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private FuelPriceRequest validRequest() {
        FuelPriceRequest request = new FuelPriceRequest();
        request.setFuelType(FuelType.MOTOR);
        request.setPricePerGallon(new BigDecimal("16330"));
        request.setValidFrom(AUGUST_1);
        return request;
    }
}