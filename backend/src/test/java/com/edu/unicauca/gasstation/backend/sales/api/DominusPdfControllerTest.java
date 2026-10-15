package com.edu.unicauca.gasstation.backend.sales.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.DominusPdfTestData;
import com.edu.unicauca.gasstation.backend.sales.application.DominusPdfService;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DominusCashPageRenderer;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** MVC slice with the actual JWT filter chain and document service, without database auto-configuration. */
@WebMvcTest(controllers = DominusPdfController.class, properties = "server.port=0")
@Import({DominusPdfService.class, DominusCashPageRenderer.class,
        SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class DominusPdfControllerTest {
    private static final String ENDPOINT = "/api/v1/sales/dominus/cash-complement";

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwt;
    @MockBean private UserDetailsService users;
    @MockBean private AuthenticationProvider authenticationProvider;

    @Test
    void adminReceivesDownloadablePdf() throws Exception {
        byte[] response = mvc.perform(multipart(ENDPOINT).file(pdf()).file(data())
                        .header("Authorization", token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dominus-2026-08-01-complemento.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument document = Loader.loadPDF(response)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }
    }

    @Test
    void rejectsAnonymousUsingExistingSecurityChain() throws Exception {
        mvc.perform(multipart(ENDPOINT).file(pdf()).file(data())).andExpect(status().isForbidden());
    }

    @Test
    void rejectsAuthenticatedUserWithoutAdminAuthority() throws Exception {
        mvc.perform(multipart(ENDPOINT).file(pdf()).file(data()).header("Authorization", token("WORKER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsMissingParts() throws Exception {
        mvc.perform(multipart(ENDPOINT).file(data()).header("Authorization", token("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MISSING_REQUEST_PART"));
        mvc.perform(multipart(ENDPOINT).file(pdf()).header("Authorization", token("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MISSING_REQUEST_PART"));
    }

    @Test
    void rejectsInvalidJsonAndMissingRequiredFields() throws Exception {
        for (String json : new String[] {"{", "null"}) {
            mvc.perform(multipart(ENDPOINT).file(pdf()).file(json(json)).header("Authorization", token("ADMIN")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart(ENDPOINT).file(pdf()).file(json("{}")).header("Authorization", token("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        String noDate = new String(DominusPdfTestData.sampleJson(), StandardCharsets.UTF_8)
                .replace("\"2026-08-01\"", "null");
        mvc.perform(multipart(ENDPOINT).file(pdf()).file(json(noDate)).header("Authorization", token("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("header.reportDate"));
    }

    @Test
    void rejectsExcessPrecisionInsteadOfRoundingAndInvalidDate() throws Exception {
        String sample = new String(DominusPdfTestData.sampleJson(), StandardCharsets.UTF_8);
        for (String invalid : new String[] {sample.replace("192000", "192000.001"),
                sample.replace("2026-08-01", "not-a-date")}) {
            mvc.perform(multipart(ENDPOINT).file(pdf()).file(json(invalid)).header("Authorization", token("ADMIN")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsEmptyAndCorruptPdfs() throws Exception {
        mvc.perform(multipart(ENDPOINT).file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                        .file(data()).header("Authorization", token("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOMINUS_EMPTY"));
        mvc.perform(multipart(ENDPOINT).file(new MockMultipartFile("file", "corrupt.pdf", "application/pdf", "%PDF-1.7\nbroken".getBytes(StandardCharsets.US_ASCII)))
                        .file(data()).header("Authorization", token("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOMINUS_INVALID_PDF"));
    }

    @Test
    void rejectsIncompatibleFileTypeAndJsonPartContentType() throws Exception {
        mvc.perform(multipart(ENDPOINT).file(new MockMultipartFile("file", "original.txt", "text/plain", new byte[] {1}))
                        .file(data()).header("Authorization", token("ADMIN")))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("DOMINUS_UNSUPPORTED_TYPE"));
        mvc.perform(multipart(ENDPOINT).file(pdf())
                        .file(new MockMultipartFile("data", "data.json", "text/plain", DominusPdfTestData.sampleJson()))
                        .header("Authorization", token("ADMIN")))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void rejectsOversizedFileBeforeReadingPdfContent() throws Exception {
        mvc.perform(multipart(ENDPOINT)
                        .file(new MockMultipartFile("file", "large.pdf", "application/pdf",
                                new byte[(int) DominusPdfService.MAX_FILE_SIZE_BYTES + 1]))
                        .file(data()).header("Authorization", token("ADMIN")))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("DOMINUS_TOO_LARGE"));
    }

    @Test
    void returnsUnprocessableEntityWhenPageCannotFit() throws Exception {
        var root = DominusPdfTestData.mapper().readTree(DominusPdfTestData.sampleJson());
        var rows = ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("expenses")).putArray("rows");
        for (int i = 0; i < 20; i++) {
            rows.addObject().put("concept", "Expense").putObject("values").put("amount", 0);
        }
        mvc.perform(multipart(ENDPOINT).file(pdf()).file(json(root.toString())).header("Authorization", token("ADMIN")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DOMINUS_LAYOUT_OVERFLOW"));
    }

    private String token(String authority) {
        var user = User.withUsername("dominus-test").password("unused").authorities(authority).build();
        when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
        return "Bearer " + jwt.generateToken(user);
    }

    private static MockMultipartFile pdf() throws Exception {
        return new MockMultipartFile("file", "dominus.pdf", "application/pdf", DominusPdfTestData.originalPdf());
    }

    private static MockMultipartFile data() throws Exception {
        return new MockMultipartFile("data", "data.json", "application/json", DominusPdfTestData.sampleJson());
    }

    private static MockMultipartFile json(String json) {
        return new MockMultipartFile("data", "data.json", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }
}
