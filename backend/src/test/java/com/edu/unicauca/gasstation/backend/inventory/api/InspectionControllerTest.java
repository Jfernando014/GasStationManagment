package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InspectionRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InspectionService;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateInspectionCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InspectionNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidInspectionException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InspectionMapperImpl;
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

import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.InspectionTestData.inspection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InspectionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(InspectionMapperImpl.class)
class InspectionControllerTest {

    private static final String URL = "/api/v1/inventory/inspections";
    private static final LocalDate EXECUTED = LocalDate.of(2026, 3, 15);
    private static final LocalDate DUE = LocalDate.of(2026, 9, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InspectionService inspectionService;

    // ---------- POST ----------

    @Test
    void createReturnsCreatedWithTheInspection() throws Exception {
        when(inspectionService.create(any(Inspection.class))).thenReturn(validInspection(1L));

        postInspection(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("CAL-ONAC-2026-08"))
                .andExpect(jsonPath("$.category").value("ONAC"))
                .andExpect(jsonPath("$.target").value("Dispensador 1 y 2 (Mangueras 1 a 6)"))
                .andExpect(jsonPath("$.dateExecuted").value("2026-03-15"))
                .andExpect(jsonPath("$.dateDue").value("2026-09-15"))
                .andExpect(jsonPath("$.status").value("VIGENTE"))
                .andExpect(jsonPath("$.certDoc").value("CERT-ONAC-0812.pdf"));
    }

    @Test
    void createSendsTheRequestDataToTheService() throws Exception {
        when(inspectionService.create(any(Inspection.class))).thenReturn(validInspection(1L));

        postInspection(validRequest()).andExpect(status().isCreated());

        verify(inspectionService).create(argThat(i -> i.getId() == null
                && i.getCode().equals("CAL-ONAC-2026-08")
                && i.getCategory() == InspectionCategory.ONAC
                && i.getDateExecuted().equals(EXECUTED)
                && i.getDateDue().equals(DUE)));
    }

    @Test
    void createRejectsAMissingCode() throws Exception {
        InspectionRequest request = validRequest();
        request.setCode("   ");

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("code"))
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.CODE_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsAMissingCategory() throws Exception {
        InspectionRequest request = validRequest();
        request.setCategory(null);

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.CATEGORY_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsAMissingExecutionDate() throws Exception {
        InspectionRequest request = validRequest();
        request.setDateExecuted(null);

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("dateExecuted"))
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.DATE_EXECUTED_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsAMissingDueDate() throws Exception {
        InspectionRequest request = validRequest();
        request.setDateDue(null);

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("dateDue"))
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.DATE_DUE_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsABlankTarget() throws Exception {
        InspectionRequest request = validRequest();
        request.setTarget("   ");

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.TARGET_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsABlankCertifier() throws Exception {
        InspectionRequest request = validRequest();
        request.setCertifier("   ");

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.CERTIFIER_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsABlankTolerance() throws Exception {
        InspectionRequest request = validRequest();
        request.setToleranceMeasured("   ");

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.TOLERANCE_REQUIRED));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createRejectsATooLongCertificateDocument() throws Exception {
        InspectionRequest request = validRequest();
        request.setCertDoc("x".repeat(201));

        postInspection(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InspectionRequest.CERT_DOC_SIZE));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void createReturnsConflictWhenTheCodeIsDuplicated() throws Exception {
        when(inspectionService.create(any(Inspection.class)))
                .thenThrow(new DuplicateInspectionCodeException("CAL-ONAC-2026-08"));

        postInspection(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        new DuplicateInspectionCodeException("CAL-ONAC-2026-08").getMessage()));
    }

    @Test
    void createReturnsBadRequestWhenTheDatesAreInvalid() throws Exception {
        when(inspectionService.create(any(Inspection.class)))
                .thenThrow(new InvalidInspectionException("The due date must be after the execution date"));

        postInspection(validRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("The due date must be after the execution date"));
    }

    // ---------- PUT ----------

    @Test
    void updateReturnsTheUpdatedInspection() throws Exception {
        when(inspectionService.update(eq(7L), any(Inspection.class))).thenReturn(validInspection(7L));

        putInspection(7L, validRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void updateReturnsNotFoundWhenTheInspectionDoesNotExist() throws Exception {
        when(inspectionService.update(eq(99L), any(Inspection.class)))
                .thenThrow(new InspectionNotFoundException(99L));

        putInspection(99L, validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(new InspectionNotFoundException(99L).getMessage()));
    }

    @Test
    void updateRejectsAnInvalidBody() throws Exception {
        InspectionRequest request = validRequest();
        request.setCategory(null);

        putInspection(7L, request).andExpect(status().isBadRequest());
        verifyNoInteractions(inspectionService);
    }

    // ---------- GET ----------

    @Test
    void findByIdReturnsTheInspection() throws Exception {
        when(inspectionService.findById(7L)).thenReturn(validInspection(7L));

        mockMvc.perform(get(URL + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.code").value("CAL-ONAC-2026-08"));
    }

    @Test
    void findByIdReturnsNotFoundWhenTheInspectionDoesNotExist() throws Exception {
        when(inspectionService.findById(99L)).thenThrow(new InspectionNotFoundException(99L));

        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound());
    }

    @Test
    void searchPassesTheFiltersToTheService() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(inspectionService.search(InspectionCategory.ONAC, InspectionStatus.VIGENTE, from, to))
                .thenReturn(List.of(validInspection(2L), validInspection(1L)));

        mockMvc.perform(get(URL)
                        .param("category", "ONAC")
                        .param("status", "VIGENTE")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void searchWithoutFiltersReturnsAnEmptyListWhenThereAreNoInspections() throws Exception {
        when(inspectionService.search(null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchRejectsAnInvalidCategory() throws Exception {
        mockMvc.perform(get(URL).param("category", "OTRA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'category' is invalid"));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void searchRejectsAnInvalidDate() throws Exception {
        mockMvc.perform(get(URL).param("from", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'from' is invalid"));
        verifyNoInteractions(inspectionService);
    }

    @Test
    void searchReturnsBadRequestWhenTheStartDateIsAfterTheEndDate() throws Exception {
        when(inspectionService.search(null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)))
                .thenThrow(new InvalidInspectionException("The start date cannot be after the end date"));

        mockMvc.perform(get(URL).param("from", "2026-09-01").param("to", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The start date cannot be after the end date"));
    }

    // ---------- helpers ----------

    private Inspection validInspection(Long id) {
        return inspection(id, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED, DUE);
    }

    private InspectionRequest validRequest() {
        InspectionRequest request = new InspectionRequest();
        request.setCode("CAL-ONAC-2026-08");
        request.setCategory(InspectionCategory.ONAC);
        request.setTarget("Dispensador 1 y 2 (Mangueras 1 a 6)");
        request.setDateExecuted(EXECUTED);
        request.setDateDue(DUE);
        request.setCertifier("Metrología del Cauca SAS (ONAC 19-LAB-042)");
        request.setToleranceMeasured("+3,8 mL (Límite ±15 mL)");
        request.setCertDoc("CERT-ONAC-0812.pdf");
        return request;
    }

    private ResultActions postInspection(InspectionRequest request) throws Exception {
        return mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putInspection(Long id, InspectionRequest request) throws Exception {
        return mockMvc.perform(put(URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
