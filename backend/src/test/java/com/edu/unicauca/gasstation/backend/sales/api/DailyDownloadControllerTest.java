package com.edu.unicauca.gasstation.backend.sales.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.FakeSellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.application.DailyDownloadService;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DailyDownloadWorkbookExporter;
import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Level 3: HTTP answers of the daily download endpoints.
 *
 * @author javiersolanop777
 */
class DailyDownloadControllerTest {

    private static final String BASE = "/api/v1/sales/downloads/";
    private MockMvc atrMockMvc;

    @BeforeEach
    void setUp() {
        DailyDownloadService objService =
                new DailyDownloadService(new FakeSellerCloseQuery(), new DailyDownloadWorkbookExporter());
        atrMockMvc = MockMvcBuilders.standaloneSetup(new DailyDownloadController(objService))
                .setControllerAdvice(new DailyDownloadExceptionHandler())
                .build();
    }

    @Test
    void adminDownloadsSalesSheet() throws Exception {
        MvcResult objResult = atrMockMvc.perform(get(BASE + "2026-10-24/sales-sheet").principal(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DailyDownloadController.XLSX))
                .andReturn();
        String varDisposition = objResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(varDisposition.contains("planilla-ventas-2026-10-24.xlsx"));
        try (XSSFWorkbook objWorkbook = new XSSFWorkbook(new ByteArrayInputStream(objResult.getResponse().getContentAsByteArray()))) {
            assertEquals("PLANILLA VENTAS", objWorkbook.getSheetAt(0).getSheetName());
        }
    }

    @Test
    void adminDownloadsLubricantsAndMisc() throws Exception {
        MvcResult objResult = atrMockMvc.perform(get(BASE + "2026-10-24/lubricants-and-misc").principal(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DailyDownloadController.XLSX))
                .andReturn();
        assertTrue(objResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)
                .contains("lubricantes-y-varios-2026-10-24.xlsx"));
    }

    @Test
    void dateWithoutDataReturns404EvenIfClientAcceptsOnlyExcel() throws Exception {
        atrMockMvc.perform(get(BASE + "2026-01-01/sales-sheet").principal(user("ADMIN"))
                        .accept(MediaType.parseMediaType(DailyDownloadController.XLSX)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DAILY_DOWNLOAD_NO_DATA"));
    }

    @Test
    void invalidDateReturns400() throws Exception {
        atrMockMvc.perform(get(BASE + "not-a-date/sales-sheet").principal(user("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE"));
    }

    @Test
    void userWithoutAdminAuthorityReturns403() throws Exception {
        atrMockMvc.perform(get(BASE + "2026-10-24/sales-sheet").principal(user("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutAuthenticationReturns401() throws Exception {
        atrMockMvc.perform(get(BASE + "2026-10-24/lubricants-and-misc"))
                .andExpect(status().isUnauthorized());
    }

    private static Principal user(String prmAuthority) {
        return new UsernamePasswordAuthenticationToken("tester", "n/a", List.of(new SimpleGrantedAuthority(prmAuthority)));
    }
}
