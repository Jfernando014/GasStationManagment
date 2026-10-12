package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.GaugeTestData;
import com.edu.unicauca.gasstation.backend.inventory.TankTestData;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.GaugePointRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.GaugeTableRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.GaugeTableService;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateGaugeTableNameException;
import com.edu.unicauca.gasstation.backend.inventory.exception.GaugeTableNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.GaugeTableMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;

@WebMvcTest(
        controllers = GaugeTableController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GaugeTableMapperImpl.class)
@TestPropertySource(properties = "server.port=0")
class GaugeTableControllerTest {

    private static final String BASE_URL = "/api/v1/inventory";
    private static final String TABLES_URL = BASE_URL + "/gauge-tables";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GaugeTableService gaugeTableService;

    // ---------- POST /gauge-tables ----------

    @Test
    void createReturnsCreatedWithTheTableAndItsLimits() throws Exception {
        when(gaugeTableService.create(any(GaugeTable.class))).thenReturn(GaugeTestData.curve(1L, "Tanques 1 y 2"));

        postTable(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tanques 1 y 2"))
                .andExpect(jsonPath("$.points.length()").value(7))
                .andExpect(jsonPath("$.minHeightCm").value(1))
                .andExpect(jsonPath("$.maxHeightCm").value(238))
                .andExpect(jsonPath("$.maxVolumeGallons").value(3090))
                .andExpect(jsonPath("$.tanks.length()").value(0));
    }

    @Test
    void createListsTheTanksThatUseTheTable() throws Exception {
        when(gaugeTableService.create(any(GaugeTable.class))).thenReturn(GaugeTestData.curve(1L, "Tanques 1 y 2"));
        when(gaugeTableService.findTanksUsing(1L))
                .thenReturn(List.of(TankTestData.tank(10L, "T1C1", "Tanque 1 Comp. 1")));

        postTable(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tanks[0].id").value(10))
                .andExpect(jsonPath("$.tanks[0].code").value("T1C1"));
    }

    @Test
    void createRejectsABlankName() throws Exception {
        GaugeTableRequest request = validRequest();
        request.setName(" ");

        postTable(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.fields[0].field").value("name"))
                .andExpect(jsonPath("$.fields[0].message").value(GaugeTableRequest.NAME_REQUIRED));
        verifyNoInteractions(gaugeTableService);
    }

    @Test
    void createRejectsMissingPoints() throws Exception {
        GaugeTableRequest request = validRequest();
        request.setPoints(null);

        postTable(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("points"))
                .andExpect(jsonPath("$.fields[0].message").value(GaugeTableRequest.POINTS_REQUIRED));
        verifyNoInteractions(gaugeTableService);
    }

    @Test
    void createRejectsAPointWithoutHeight() throws Exception {
        GaugeTableRequest request = validRequest();
        request.getPoints().get(0).setHeightCm(null);

        postTable(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("points[0].heightCm"))
                .andExpect(jsonPath("$.fields[0].message").value(GaugePointRequest.HEIGHT_REQUIRED));
        verifyNoInteractions(gaugeTableService);
    }

    @Test
    void createReturnsBadRequestWithTheDetailOfTheInvalidPoint() throws Exception {
        when(gaugeTableService.create(any(GaugeTable.class)))
                .thenThrow(new InvalidGaugeTableException("points[1]", "detalle del punto"));

        postTable(validRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("detalle del punto"))
                .andExpect(jsonPath("$.fields[0].field").value("points[1]"))
                .andExpect(jsonPath("$.fields[0].message").value("detalle del punto"));
    }

    @Test
    void createReturnsConflictWhenTheNameAlreadyExists() throws Exception {
        when(gaugeTableService.create(any(GaugeTable.class)))
                .thenThrow(new DuplicateGaugeTableNameException("Tanques 1 y 2"));

        postTable(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(new DuplicateGaugeTableNameException("Tanques 1 y 2").getMessage()));
    }

    // ---------- POST /gauge-tables/import ----------

    @Test
    void createFromCsvReturnsCreated() throws Exception {
        when(gaugeTableService.createFromCsv(eq("Tanques 1 y 2"), any(String.class)))
                .thenReturn(GaugeTestData.curve(1L, "Tanques 1 y 2"));

        mockMvc.perform(multipart(TABLES_URL + "/import").file(csvFile("1,1\n2,4\n")).param("name", "Tanques 1 y 2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        verify(gaugeTableService).createFromCsv("Tanques 1 y 2", "1,1\n2,4\n");
    }

    @Test
    void createFromCsvReturnsBadRequestWhenTheFileHasAnInvalidLine() throws Exception {
        when(gaugeTableService.createFromCsv(eq("Tanques 1 y 2"), any(String.class)))
                .thenThrow(new InvalidGaugeTableException("line[2]", "detalle de la línea"));

        mockMvc.perform(multipart(TABLES_URL + "/import").file(csvFile("1,1\n2,xyz")).param("name", "Tanques 1 y 2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("line[2]"));
    }

    @Test
    void createFromCsvRejectsARequestWithoutFile() throws Exception {
        // El handler no cubre MissingServletRequestPartException: responde el 400 por defecto de Spring
        mockMvc.perform(multipart(TABLES_URL + "/import").param("name", "Tanques 1 y 2"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(gaugeTableService);
    }

    // ---------- GET ----------

    @Test
    void findAllReturnsTheTables() throws Exception {
        when(gaugeTableService.findAll()).thenReturn(List.of(
                GaugeTestData.curve(1L, "Tanques 1 y 2"), GaugeTestData.curve(2L, "Tanques 3 y 4")));

        mockMvc.perform(get(TABLES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Tanques 1 y 2"))
                .andExpect(jsonPath("$[1].name").value("Tanques 3 y 4"));
    }

    @Test
    void findByIdReturnsTheTable() throws Exception {
        when(gaugeTableService.findById(1L)).thenReturn(GaugeTestData.curve(1L, "Tanques 1 y 2"));

        mockMvc.perform(get(TABLES_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tanques 1 y 2"))
                .andExpect(jsonPath("$.points[4].heightCm").value(55))
                .andExpect(jsonPath("$.points[4].volumeGallons").value(559));
    }

    @Test
    void findByIdReturnsNotFoundWhenTheTableDoesNotExist() throws Exception {
        when(gaugeTableService.findById(99L)).thenThrow(new GaugeTableNotFoundException(99L));

        mockMvc.perform(get(TABLES_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value(new GaugeTableNotFoundException(99L).getMessage()))
                .andExpect(jsonPath("$.path").value(TABLES_URL + "/99"));
    }

    // ---------- PUT /gauge-tables/{id} ----------

    @Test
    void updateReturnsTheUpdatedTable() throws Exception {
        when(gaugeTableService.update(eq(1L), any(GaugeTable.class))).thenReturn(GaugeTestData.curve(1L, "Tanques 1 y 2"));

        putTable(1L, validRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsNotFoundWhenTheTableDoesNotExist() throws Exception {
        when(gaugeTableService.update(eq(99L), any(GaugeTable.class))).thenThrow(new GaugeTableNotFoundException(99L));

        putTable(99L, validRequest()).andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsConflictWhenAnotherTableUsesTheName() throws Exception {
        when(gaugeTableService.update(eq(1L), any(GaugeTable.class)))
                .thenThrow(new DuplicateGaugeTableNameException("Tanques 1 y 2"));

        putTable(1L, validRequest()).andExpect(status().isConflict());
    }

    @Test
    void updateRejectsAnInvalidBody() throws Exception {
        GaugeTableRequest request = validRequest();
        request.setName("");

        putTable(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("name"));
        verifyNoInteractions(gaugeTableService);
    }

    // ---------- PUT /gauge-tables/{id}/import ----------

    @Test
    void replaceFromCsvReturnsTheUpdatedTable() throws Exception {
        when(gaugeTableService.replacePointsFromCsv(eq(1L), any(String.class)))
                .thenReturn(GaugeTestData.curve(1L, "Tanques 1 y 2"));

        mockMvc.perform(multipart(HttpMethod.PUT, TABLES_URL + "/1/import").file(csvFile("1,1\n2,4\n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(gaugeTableService).replacePointsFromCsv(1L, "1,1\n2,4\n");
    }

    @Test
    void replaceFromCsvReturnsNotFoundWhenTheTableDoesNotExist() throws Exception {
        when(gaugeTableService.replacePointsFromCsv(eq(99L), any(String.class)))
                .thenThrow(new GaugeTableNotFoundException(99L));

        mockMvc.perform(multipart(HttpMethod.PUT, TABLES_URL + "/99/import").file(csvFile("1,1\n2,4\n")))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /gauge-tables/{id}/export ----------

    @Test
    void exportReturnsTheCsvAsAnAttachment() throws Exception {
        String csv = "Altura_cm,Volumen_Galones\n1,1\n2,4\n";
        GaugeTable table = GaugeTestData.curve(1L, "Tanques 1 y 2");
        when(gaugeTableService.findById(1L)).thenReturn(table);
        when(gaugeTableService.toCsv(table)).thenReturn(csv);

        mockMvc.perform(get(TABLES_URL + "/1/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Tabla Aforo.csv\""))
                .andExpect(content().string(csv));
    }

    @Test
    void exportReturnsNotFoundWhenTheTableDoesNotExist() throws Exception {
        when(gaugeTableService.findById(99L)).thenThrow(new GaugeTableNotFoundException(99L));

        mockMvc.perform(get(TABLES_URL + "/99/export"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    // ---------- GET /tanks/{tankId}/volume ----------

    @Test
    void volumeReturnsTheCalculatedGallons() throws Exception {
        when(gaugeTableService.calculateVolume(1L, new BigDecimal("55"))).thenReturn(new BigDecimal("539.00"));

        mockMvc.perform(get(BASE_URL + "/tanks/1/volume").param("heightCm", "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tankId").value(1))
                .andExpect(jsonPath("$.heightCm").value(55))
                .andExpect(jsonPath("$.volumeGallons").value(539.0));
    }

    @Test
    void volumeAcceptsAHeightWithOneDecimal() throws Exception {
        when(gaugeTableService.calculateVolume(1L, new BigDecimal("55.5"))).thenReturn(new BigDecimal("546.50"));

        mockMvc.perform(get(BASE_URL + "/tanks/1/volume").param("heightCm", "55.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volumeGallons").value(546.5));
    }

    @Test
    void volumeRejectsANegativeHeight() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tanks/1/volume").param("heightCm", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("One or more parameters failed validation"))
                .andExpect(jsonPath("$.fields[0].field").value("heightCm"))
                .andExpect(jsonPath("$.fields[0].message").value("The height cannot be negative"));
        verifyNoInteractions(gaugeTableService);
    }

    @Test
    void volumeRejectsAMissingHeight() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tanks/1/volume"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("The required parameter is missing 'heightCm'"));
        verifyNoInteractions(gaugeTableService);
    }

    @Test
    void volumeRejectsANonNumericHeight() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tanks/1/volume").param("heightCm", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'heightCm' is invalid"));
        verifyNoInteractions(gaugeTableService);
    }

    @Test
    void volumeReturnsBadRequestWhenTheHeightExceedsTheTable() throws Exception {
        when(gaugeTableService.calculateVolume(1L, new BigDecimal("300")))
                .thenThrow(new HeightExceedsGaugeException(new BigDecimal("238")));

        mockMvc.perform(get(BASE_URL + "/tanks/1/volume").param("heightCm", "300"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value(new HeightExceedsGaugeException(new BigDecimal("238")).getMessage()));
    }

    @Test
    void volumeReturnsNotFoundWhenTheTankDoesNotExist() throws Exception {
        when(gaugeTableService.calculateVolume(99L, new BigDecimal("55"))).thenThrow(new TankNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/tanks/99/volume").param("heightCm", "55"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    @Test
    void volumeReturnsConflictWhenTheTankHasNoGaugeTable() throws Exception {
        when(gaugeTableService.calculateVolume(1L, new BigDecimal("55")))
                .thenThrow(new TankWithoutGaugeTableException(1L));

        mockMvc.perform(get(BASE_URL + "/tanks/1/volume").param("heightCm", "55"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(new TankWithoutGaugeTableException(1L).getMessage()));
    }

    // ---------- helpers ----------

    private GaugeTableRequest validRequest() {
        GaugeTableRequest request = new GaugeTableRequest();
        request.setName("Tanques 1 y 2");
        request.setPoints(new java.util.ArrayList<>(List.of(
                pointRequest("1", "1"), pointRequest("55", "559"), pointRequest("238", "3090"))));
        return request;
    }

    private GaugePointRequest pointRequest(String heightCm, String volumeGallons) {
        GaugePointRequest point = new GaugePointRequest();
        point.setHeightCm(new BigDecimal(heightCm));
        point.setVolumeGallons(new BigDecimal(volumeGallons));
        return point;
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "aforo.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions postTable(GaugeTableRequest request) throws Exception {
        return mockMvc.perform(post(TABLES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putTable(Long id, GaugeTableRequest request) throws Exception {
        return mockMvc.perform(put(TABLES_URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}