package com.edu.unicauca.gasstation.backend.workers.api;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerChanges;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDetail;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.domain.services.WorkerService;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.exception.RoleNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.mappers.WorkerMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(
        controllers = WorkerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(WorkerMapperImpl.class)
@TestPropertySource(properties = "server.port=0")
class WorkerControllerTest {

    private static final String BASE_URL = "/api/workers";
    private static final UUID WORKER_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkerService workerService;

    // ---- POST ----

    @Test
    void createReturnsCreatedWithTheWorkerAndRole() throws Exception {
        when(workerService.create(any(Worker.class))).thenReturn(anaDetail(true));

        postJson(body("  ana   villamil ", "0012345678", TITULAR_ROLE_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(WORKER_ID.toString()))
                .andExpect(jsonPath("$.document").value("0012345678"))
                .andExpect(jsonPath("$.roleName").value("TITULAR"))
                .andExpect(jsonPath("$.dispenser").value(1))
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<Worker> captor = ArgumentCaptor.forClass(Worker.class);
        verify(workerService).create(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("Ana Villamil");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void createRejectsInvalidBody(String description, Map<String, Object> body, String field, String message)
            throws Exception {
        postJson(body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", everyItem(is(field))))
                .andExpect(jsonPath("$.errors[*].message", hasItem(message)));
        verifyNoInteractions(workerService);
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\": \"Ana\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        verifyNoInteractions(workerService);
    }

    @Test
    void createReturnsConflictWhenDocumentAlreadyExists() throws Exception {
        when(workerService.create(any(Worker.class))).thenThrow(new DuplicateDocumentException("0012345678"));

        postJson(body("Ana Villamil", "0012345678", TITULAR_ROLE_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_DOCUMENT"))
                .andExpect(jsonPath("$.detail").value(DuplicateDocumentException.MESSAGE));
    }

    @Test
    void createReturnsBadRequestWhenRoleDoesNotExist() throws Exception {
        when(workerService.create(any(Worker.class))).thenThrow(new RoleNotFoundException(TITULAR_ROLE_ID));

        postJson(body("Ana Villamil", "0012345678", TITULAR_ROLE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("El rol indicado no existe"));
    }

    // ---- PUT ----

    @Test
    void updateReturnsTheUpdatedWorker() throws Exception {
        when(workerService.update(eq(WORKER_ID), any(WorkerChanges.class))).thenReturn(anaDetail(true));

        putJson(WORKER_ID.toString(), body(" Ana Villamil ", "0012345678", TITULAR_ROLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Villamil"));

        verify(workerService).update(WORKER_ID, new WorkerChanges("Ana Villamil", "0012345678", TITULAR_ROLE_ID));
    }

    @Test
    void updateReturnsNotFoundWhenWorkerDoesNotExist() throws Exception {
        when(workerService.update(eq(WORKER_ID), any(WorkerChanges.class)))
                .thenThrow(new WorkerNotFoundException(WORKER_ID));

        putJson(WORKER_ID.toString(), body("Ana Villamil", "0012345678", TITULAR_ROLE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKER_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No se encontró el trabajador con id " + WORKER_ID));
    }

    @Test
    void updateRejectsAnIdThatIsNotAUuid() throws Exception {
        putJson("abc", body("Ana Villamil", "0012345678", TITULAR_ROLE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail").value("El valor del parámetro 'id' no es válido"));
        verifyNoInteractions(workerService);
    }

    // ---- PATCH /{id}/status ----

    @Test
    void changeStatusReturnsTheWorkerWithTheNewStatus() throws Exception {
        when(workerService.changeStatus(WORKER_ID, false)).thenReturn(anaDetail(false));

        patchStatus("{\"active\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void changeStatusRejectsAMissingActive() throws Exception {
        patchStatus("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("active"))
                .andExpect(jsonPath("$.errors[0].message").value("El estado es obligatorio"));
        verifyNoInteractions(workerService);
    }

    // ---- GET ----

    @Test
    void getByIdReturnsNotFoundWhenWorkerDoesNotExist() throws Exception {
        when(workerService.getById(WORKER_ID)).thenThrow(new WorkerNotFoundException(WORKER_ID));

        mockMvc.perform(get(BASE_URL + "/" + WORKER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKER_NOT_FOUND"));
    }

    @Test
    void listPassesEveryParameterToTheFilter() throws Exception {
        when(workerService.list(any(WorkerFilter.class))).thenReturn(List.of(anaDetail(true)));

        mockMvc.perform(get(BASE_URL)
                        .param("active", "true")
                        .param("roleId", SUPPORT_ROLE_ID.toString())
                        .param("search", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleName").value("TITULAR"));

        verify(workerService).list(new WorkerFilter(true, SUPPORT_ROLE_ID, "ana"));
    }

    @Test
    void listRejectsAnActiveValueThatIsNotABoolean() throws Exception {
        mockMvc.perform(get(BASE_URL).param("active", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        verifyNoInteractions(workerService);
    }

    @Test
    void deleteIsNotAllowed() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + WORKER_ID)).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(workerService);
    }

    // ---- helpers ----

    /** One body per validation rule: description, body, invalid field and message. */
    static Stream<Arguments> invalidBodies() {
        String name = "Ana Villamil";
        String document = "0012345678";
        return Stream.of(
                Arguments.of("blank name", body("   ", document, TITULAR_ROLE_ID),
                        "fullName", "El nombre completo es obligatorio"),
                Arguments.of("name with digits", body("Juan 123", document, TITULAR_ROLE_ID),
                        "fullName", "El nombre solo puede tener letras, espacios, guiones o apóstrofos"),
                Arguments.of("missing document", body(name, null, TITULAR_ROLE_ID),
                        "document", "El documento es obligatorio"),
                Arguments.of("document under 6 digits", body(name, "12345", TITULAR_ROLE_ID),
                        "document", "El documento debe tener entre 6 y 12 caracteres"),
                Arguments.of("document with dots", body(name, "12.345.678", TITULAR_ROLE_ID),
                        "document", "El documento solo puede tener números, sin puntos, guiones ni espacios"),
                Arguments.of("missing role", body(name, document, null),
                        "roleId", "El rol es obligatorio"));
    }

    /** JSON body for POST and PUT; null values are sent as null. */
    private static Map<String, Object> body(String fullName, String document, UUID roleId) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", fullName);
        body.put("document", document);
        body.put("roleId", roleId);
        return body;
    }

    private static WorkerDetail anaDetail(boolean active) {
        Worker ana = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, active);
        return WorkerTestData.detail(ana, WorkerTestData.titularRole());
    }

    private ResultActions postJson(Object body) throws Exception {
        return mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions putJson(String id, Object body) throws Exception {
        return mockMvc.perform(put(BASE_URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions patchStatus(String json) throws Exception {
        return mockMvc.perform(patch(BASE_URL + "/" + WORKER_ID + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
