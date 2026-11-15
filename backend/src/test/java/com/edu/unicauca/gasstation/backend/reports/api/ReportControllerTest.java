package com.edu.unicauca.gasstation.backend.reports.api;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.NEXT_DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.balanceReport;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.dominusReport;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.variationReport;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.reports.application.BalanceReportService;
import com.edu.unicauca.gasstation.backend.reports.application.DominusComplementReportService;
import com.edu.unicauca.gasstation.backend.reports.application.VariationReportService;
import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.DominusComplementReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import com.edu.unicauca.gasstation.backend.reports.exception.InvalidReportPeriodException;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.BalancePresentationExporter;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.DominusComplementWorkbookExporter;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.VariationWorkbookExporter;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the report download endpoints, their ADMIN guard and their error mapping.
 *
 * @author javiersolanop777
 */
class ReportControllerTest {

    private static final String BASE = "/api/v1/reports/";
    private MockMvc mvc;
    private VariationReportService variationService;
    private DominusComplementReportService dominusService;
    private BalanceReportService balanceService;
    private VariationWorkbookExporter variationExporter;
    private DominusComplementWorkbookExporter dominusExporter;

    @BeforeEach
    void setUp() throws IOException {
        variationService = mock(VariationReportService.class);
        dominusService = mock(DominusComplementReportService.class);
        balanceService = mock(BalanceReportService.class);
        variationExporter = mock(VariationWorkbookExporter.class);
        dominusExporter = mock(DominusComplementWorkbookExporter.class);
        BalancePresentationExporter balanceExporter = mock(BalancePresentationExporter.class);

        when(variationService.build(DATE)).thenReturn(variationReport());
        when(dominusService.build(DATE)).thenReturn(dominusReport());
        when(balanceService.build(DATE, NEXT_DATE)).thenReturn(balanceReport());
        when(variationExporter.export(any(VariationReport.class))).thenReturn(new byte[]{1, 2, 3});
        when(dominusExporter.export(any(DominusComplementReport.class))).thenReturn(new byte[]{7, 8, 9});
        when(balanceExporter.export(any(BalanceReport.class))).thenReturn(new byte[]{4, 5, 6});

        mvc = MockMvcBuilders.standaloneSetup(new ReportController(variationService, dominusService,
                        balanceService, variationExporter, dominusExporter, balanceExporter))
                .setControllerAdvice(new ReportExceptionHandler())
                .build();
    }

    @Test
    void adminDownloadsVariationExcel() throws Exception {
        MvcResult result = mvc.perform(get(BASE + "variation/2026-08-01").principal(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReportController.XLSX))
                .andReturn();

        assertTrue(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)
                .contains("variacion-2026-08-01.xlsx"));
    }

    @Test
    void adminDownloadsDominusComplementExcel() throws Exception {
        MvcResult result = mvc.perform(get(BASE + "dominus-complement/2026-08-01").principal(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReportController.XLSX))
                .andReturn();

        assertTrue(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)
                .contains("complemento-dominus-2026-08-01.xlsx"));
    }

    @Test
    void adminDownloadsBalancePowerPoint() throws Exception {
        MvcResult result = mvc.perform(get(BASE + "balance")
                        .param("from", "2026-08-01").param("to", "2026-08-02")
                        .principal(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReportController.PPTX))
                .andReturn();

        assertTrue(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)
                .contains("balance-operativo-2026-08-01_2026-08-02.pptx"));
    }

    @Test
    void noDataReturns404WithTheReportCode() throws Exception {
        when(variationService.build(DATE)).thenThrow(new ReportNoDataException(
                "No hay información suficiente para generar el reporte de esta fecha"));

        mvc.perform(get(BASE + "variation/2026-08-01").principal(user("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NO_DATA"));
    }

    @Test
    void invalidPeriodReturns400() throws Exception {
        when(balanceService.build(DATE, NEXT_DATE))
                .thenThrow(new InvalidReportPeriodException("Período inválido"));

        mvc.perform(get(BASE + "balance").param("from", "2026-08-01").param("to", "2026-08-02")
                        .principal(user("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
    }

    @Test
    void invalidDateReturns400() throws Exception {
        mvc.perform(get(BASE + "variation/not-a-date").principal(user("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE"));
    }

    @Test
    void missingBalanceParameterReturns400() throws Exception {
        mvc.perform(get(BASE + "balance").param("from", "2026-08-01").principal(user("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    void generationFailureReturns500() throws Exception {
        when(variationExporter.export(any(VariationReport.class))).thenThrow(new IOException("boom"));

        mvc.perform(get(BASE + "variation/2026-08-01").principal(user("ADMIN")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("REPORT_GENERATION_FAILED"));
    }

    @Test
    void userWithoutAdminAuthorityReturns403() throws Exception {
        mvc.perform(get(BASE + "variation/2026-08-01").principal(user("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutAuthenticationReturns401() throws Exception {
        mvc.perform(get(BASE + "variation/2026-08-01"))
                .andExpect(status().isUnauthorized());
    }

    private static Principal user(String authority) {
        return new UsernamePasswordAuthenticationToken("tester", "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
