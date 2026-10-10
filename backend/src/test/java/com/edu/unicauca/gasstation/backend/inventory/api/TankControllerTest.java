package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.TankTestData;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankRequest;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.TankService;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateTankCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.TankMapperImpl;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TankController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)

@Import(TankMapperImpl.class)
@TestPropertySource(properties = "server.port=0")
class TankControllerTest {

    private static final String BASE_URL = "/api/v1/inventory/tanks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TankService tankService;

    @Test
    void createTankReturnsCreatedWithTheTank() throws Exception {
        when(tankService.createTank(any(Tank.class)))
                .thenReturn(TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1"));

        postTank(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("T1C1"))
                .andExpect(jsonPath("$.fuelType").value("MOTOR"));
    }

    @Test
    void createTankRejectsNonPositiveCapacity() throws Exception {
        TankRequest request = validRequest();
        request.setMaxCapacityGallons(new BigDecimal("-5"));

        postTank(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("maxCapacityGallons"))
                .andExpect(jsonPath("$.fields[0].message").value("The capacity must be a positive numerical value"));
        verifyNoInteractions(tankService);
    }

    @Test
    void createTankRejectsBlankName() throws Exception {
        TankRequest request = validRequest();
        request.setName("");

        postTank(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("name"))
                .andExpect(jsonPath("$.fields[0].message").value("The NAME and CAPACITY fields are required"));
        verifyNoInteractions(tankService);
    }

    @Test
    void createTankRejectsMissingCapacity() throws Exception {
        TankRequest request = validRequest();
        request.setMaxCapacityGallons(null);

        postTank(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("maxCapacityGallons"))
                .andExpect(jsonPath("$.fields[0].message").value("The NAME and CAPACITY fields are required"));
        verifyNoInteractions(tankService);
    }

    @Test
    void createTankRejectsNegativeTolerance() throws Exception {
        TankRequest request = validRequest();
        request.setToleranceCm(new BigDecimal("-1"));

        postTank(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("toleranceCm"))
                .andExpect(jsonPath("$.fields[0].message").value("The tolerance must be a numeric value greater than or equal to zero"));
        verifyNoInteractions(tankService);
    }

    @Test
    void createTankRejectsNonNumericCapacity() throws Exception {
        String json = """
                {"code":"T1C1","name":"Tanque 1","fuelType":"MOTOR",
                 "maxCapacityGallons":"abc","maxHeightCm":238,"toleranceCm":1}
                """;

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid data"));
        verifyNoInteractions(tankService);
    }

    @Test
    void createTankReturnsConflictWhenCodeAlreadyExists() throws Exception {
        when(tankService.createTank(any(Tank.class))).thenThrow(new DuplicateTankCodeException("T1C1"));

        postTank(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A tank with code T1C1 already exists"));
    }

    @Test
    void findAllReturnsTheTanks() throws Exception {
        when(tankService.findAllTanks()).thenReturn(List.of(
                TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1"),
                TankTestData.tank(2L, "T1C2", "Tanque 1 Comp. 2")));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("T1C1"));
    }

    @Test
    void findByIdReturnsNotFoundWhenTankDoesNotExist() throws Exception {
        when(tankService.findTankById(99L)).thenThrow(new TankNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tank with code 99 not found"));
    }

    @Test
    void updateTankReturnsTheUpdatedTank() throws Exception {
        when(tankService.updateTank(eq(1L), any(Tank.class)))
                .thenReturn(TankTestData.tank(1L, "T1C1", "Nombre nuevo"));
        TankRequest request = validRequest();
        request.setName("Nombre nuevo");

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre nuevo"));
    }

    private ResultActions postTank(TankRequest request) throws Exception {
        return mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private TankRequest validRequest() {
        TankRequest request = new TankRequest();
        request.setCode("T1C1");
        request.setName("Tanque 1 Comp. 1");
        request.setFuelType(FuelType.MOTOR);
        request.setMaxCapacityGallons(new BigDecimal("3090"));
        request.setMaxHeightCm(new BigDecimal("238"));
        request.setToleranceCm(new BigDecimal("1"));
        return request;
    }
}