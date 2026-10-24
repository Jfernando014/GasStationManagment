package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankMeasurementRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankReadingRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.TankMeasurementService;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidTankMeasurementException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankMeasurementNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.TankMeasurementMapperImpl;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.context.TestPropertySource;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TankMeasurementController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(TankMeasurementMapperImpl.class)
class TankMeasurementControllerTest {

    private static final String URL = "/api/v1/inventory/measurements";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TankMeasurementService measurementService;

    // ---------- PUT ----------

    @Test
    void saveReturnsTheMeasurementWithTheCalculatedGallons() throws Exception {
        when(measurementService.save(eq(DATE), anyList()))
                .thenReturn(measurement(1L, DATE, reading(1L, "55", "539.00")));

        put(DATE, body(1L, "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.journalDate").value("2026-08-05"))
                .andExpect(jsonPath("$.readings.length()").value(1))
                .andExpect(jsonPath("$.readings[0].tankId").value(1))
                .andExpect(jsonPath("$.readings[0].heightCm").value(55))
                .andExpect(jsonPath("$.readings[0].volumeGallons").value(539.0));
    }

    @Test
    void saveSendsTheReadingsToTheServiceWithoutGallons() throws Exception {
        when(measurementService.save(eq(DATE), anyList())).thenReturn(measurement(1L, DATE, reading(1L, "55", "539")));

        put(DATE, body(1L, "55")).andExpect(status().isOk());

        verify(measurementService).save(eq(DATE), argThat(readings -> readings.size() == 1
                && readings.get(0).getTankId().equals(1L)
                && readings.get(0).getHeightCm().compareTo(new BigDecimal("55")) == 0
                && readings.get(0).getVolumeGallons() == null));
    }

    @Test
    void saveAcceptsAHeightOfZero() throws Exception {
        when(measurementService.save(eq(DATE), anyList())).thenReturn(measurement(1L, DATE, reading(1L, "0", "0")));

        put(DATE, body(1L, "0")).andExpect(status().isOk());
    }

    @Test
    void saveRejectsANegativeHeight() throws Exception {
        put(DATE, body(1L, "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.fields[0].field").value("readings[0].heightCm"))
                .andExpect(jsonPath("$.fields[0].message").value(TankReadingRequest.HEIGHT_NOT_NEGATIVE));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveRejectsAMissingHeight() throws Exception {
        put(DATE, body(1L, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("readings[0].heightCm"))
                .andExpect(jsonPath("$.fields[0].message").value(TankReadingRequest.HEIGHT_REQUIRED));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveRejectsMoreThanOneDecimalInTheHeight() throws Exception {
        put(DATE, body(1L, "55.55"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(TankReadingRequest.HEIGHT_DIGITS));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveRejectsAMissingTank() throws Exception {
        put(DATE, body(null, "55"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("readings[0].tankId"))
                .andExpect(jsonPath("$.fields[0].message").value(TankReadingRequest.TANK_REQUIRED));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveRejectsNoReadings() throws Exception {
        put(DATE, "{\"readings\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("readings"))
                .andExpect(jsonPath("$.fields[0].message").value(TankMeasurementRequest.READINGS_REQUIRED));
        put(DATE, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(TankMeasurementRequest.READINGS_REQUIRED));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveRejectsAHeightThatIsNotANumber() throws Exception {
        put(DATE, "{\"readings\":[{\"tankId\":1,\"heightCm\":\"abc\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveRejectsAnInvalidDateInThePath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, "55")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'journalDate' is invalid"));
        verifyNoInteractions(measurementService);
    }

    @Test
    void saveReturnsBadRequestWhenTheServiceRejectsTheMeasurement() throws Exception {
        when(measurementService.save(eq(DATE), anyList()))
                .thenThrow(new InvalidTankMeasurementException("The measurement includes the same tank more than once"));

        put(DATE, body(1L, "55"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("The measurement includes the same tank more than once"));
    }

    @Test
    void saveReturnsBadRequestWhenTheHeightExceedsTheGaugeTable() throws Exception {
        HeightExceedsGaugeException exception = new HeightExceedsGaugeException(new BigDecimal("238"));
        when(measurementService.save(eq(DATE), anyList())).thenThrow(exception);

        put(DATE, body(1L, "300"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));
    }

    @Test
    void saveReturnsNotFoundForAnUnknownTank() throws Exception {
        when(measurementService.save(eq(DATE), anyList())).thenThrow(new TankNotFoundException(99L));

        put(DATE, body(99L, "55"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value(new TankNotFoundException(99L).getMessage()));
    }

    @Test
    void saveReturnsConflictWhenTheTankHasNoGaugeTable() throws Exception {
        when(measurementService.save(eq(DATE), anyList())).thenThrow(new TankWithoutGaugeTableException(1L));

        put(DATE, body(1L, "55"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(new TankWithoutGaugeTableException(1L).getMessage()));
    }

    // ---------- GET ----------

    @Test
    void findByJournalDateReturnsTheMeasurement() throws Exception {
        when(measurementService.findByJournalDate(DATE))
                .thenReturn(Optional.of(measurement(1L, DATE, reading(1L, "55", "539.00"), reading(2L, "10", "30.00"))));

        mockMvc.perform(get(URL + "/2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalDate").value("2026-08-05"))
                .andExpect(jsonPath("$.readings.length()").value(2))
                .andExpect(jsonPath("$.readings[1].tankId").value(2));
    }

    @Test
    void findByJournalDateReturnsNotFoundWhenThereIsNoMeasurement() throws Exception {
        when(measurementService.findByJournalDate(DATE)).thenReturn(Optional.empty());

        mockMvc.perform(get(URL + "/2026-08-05"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value(TankMeasurementNotFoundException.forDate(DATE).getMessage()));
    }

    @Test
    void previousReturnsTheLastMeasurementBeforeTheDate() throws Exception {
        when(measurementService.findLatestBefore(DATE.plusDays(1)))
                .thenReturn(Optional.of(measurement(1L, DATE, reading(1L, "55", "539.00"))));

        mockMvc.perform(get(URL + "/previous").param("journalDate", "2026-08-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalDate").value("2026-08-05"));
    }

    @Test
    void previousReturnsNotFoundWhenThereIsNoEarlierMeasurement() throws Exception {
        when(measurementService.findLatestBefore(DATE)).thenReturn(Optional.empty());

        mockMvc.perform(get(URL + "/previous").param("journalDate", "2026-08-05"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(TankMeasurementNotFoundException.before(DATE).getMessage()));
    }

    @Test
    void previousRequiresTheJournalDate() throws Exception {
        mockMvc.perform(get(URL + "/previous"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The required parameter is missing 'journalDate'"));
        verifyNoInteractions(measurementService);
    }

    // ---------- helpers ----------

    private ResultActions put(LocalDate date, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put(URL + "/" + date).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String body(Long tankId, String heightCm) throws Exception {
        TankReadingRequest reading = new TankReadingRequest();
        reading.setTankId(tankId);
        reading.setHeightCm(heightCm == null ? null : new BigDecimal(heightCm));
        TankMeasurementRequest request = new TankMeasurementRequest();
        request.setReadings(List.of(reading));
        return objectMapper.writeValueAsString(request);
    }
}