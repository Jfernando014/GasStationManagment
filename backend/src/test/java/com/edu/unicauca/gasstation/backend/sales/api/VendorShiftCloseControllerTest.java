package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.application.SellerCloseQueryServiceImpl;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.security.infrastructure.config.SecurityConfig;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VendorShiftCloseController.class, properties = "server.port=0")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, JournalExceptionHandler.class})
class VendorShiftCloseControllerTest {

    private static final String BASE = "/api/v1/sales/vendor-closes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SellerCloseQueryServiceImpl sellerCloseQueryService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    private String bearer(String authority) {
        var user = User.withUsername("tester").password("unused").authorities(authority).build();
        when(userDetailsService.loadUserByUsername("tester")).thenReturn(user);
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    @DisplayName("GET /fuel-prices returns effective fuel prices map for requested date")
    void getEffectiveFuelPrices_returnsPricesMap() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 19);
        when(sellerCloseQueryService.getEffectiveFuelPrices(date)).thenReturn(Map.of(
                "MOTOR", new BigDecimal("15826.00"),
                "DIESEL", new BigDecimal("11484.00"),
                "EXTRA", new BigDecimal("18500.00"),
                "MAX_PRO", new BigDecimal("17100.00")
        ));

        mockMvc.perform(get(BASE + "/fuel-prices")
                        .param("date", "2026-11-19")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MOTOR").value(15826.00))
                .andExpect(jsonPath("$.DIESEL").value(11484.00))
                .andExpect(jsonPath("$.EXTRA").value(18500.00))
                .andExpect(jsonPath("$.MAX_PRO").value(17100.00));

        verify(sellerCloseQueryService).getEffectiveFuelPrices(date);
    }

    @Test
    @DisplayName("POST /vendor-closes persists valid batch and returns summaries")
    void registerVendorCloses_returns200_whenBatchIsValid() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 19);
        VendorCloseSummary summary = new VendorCloseSummary(
                date,
                "52875796",
                "Mónica Dorado",
                "12-7",
                3,
                new BigDecimal("2373000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1354436.00"),
                new BigDecimal("162606.00"),
                BigDecimal.ZERO,
                new BigDecimal("3890042.00"),
                new BigDecimal("223.701"),
                new BigDecimal("13.413"),
                new BigDecimal("2.755"),
                BigDecimal.ZERO,
                new BigDecimal("20800.00"),
                new BigDecimal("3890028.87"),
                new BigDecimal("13.13"),
                "SOBRA",
                List.of(new ProductSaleSummary(
                        "76102",
                        "Aditivo Gasolina Ruta 75 40ml",
                        "ADITIVO",
                        2,
                        new BigDecimal("6800.00"),
                        new BigDecimal("13600.00")
                ))
        );
        when(sellerCloseQueryService.registerVendorCloses(eq(date), anyList())).thenReturn(List.of(
                new VendorShiftClose(
                        UUID.randomUUID(), date, "52875796", "Mónica Dorado", "12-7", 3,
                        new BigDecimal("2373000.00"), BigDecimal.ZERO, new BigDecimal("1354436.00"),
                        new BigDecimal("162606.00"), BigDecimal.ZERO, new BigDecimal("3890042.00"),
                        new BigDecimal("223.701"), new BigDecimal("13.413"), new BigDecimal("2.755"),
                        BigDecimal.ZERO, new BigDecimal("20800.00"), new BigDecimal("3890028.87"),
                        new BigDecimal("13.13"), new BigDecimal("0.50"), DiscrepancyStatus.SOBRA, List.of()
                )
        ));
        when(sellerCloseQueryService.findVendorClosesByDate(date)).thenReturn(List.of(summary));

        String validBody = """
                {
                  "journalDate": "2026-11-19",
                  "closes": [
                    {
                      "vendorDocument": "52875796",
                      "vendorName": "Mónica Dorado",
                      "shiftName": "12-7",
                      "pumpNumber": 3,
                      "cash": 2373000,
                      "ga": 0,
                      "credibanco": 1354436,
                      "qr": 162606,
                      "redemptions": 0,
                      "totalDelivered": 3890042,
                      "gallonsMotor": 223.701,
                      "gallonsDiesel": 13.413,
                      "gallonsExtra": 2.755,
                      "gallonsMaxpro": 0,
                      "basketDominusAmount": 20800,
                      "dominusTirilla": 3890028.87,
                      "products": [
                        {
                          "sku": "76102",
                          "productName": "Aditivo Gasolina Ruta 75 40ml",
                          "category": "ADITIVO",
                          "unitsSold": 2,
                          "unitPrice": 6800,
                          "totalAmount": 13600
                        }
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post(BASE)
                        .param("date", "2026-11-19")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vendorDocument").value("52875796"))
                .andExpect(jsonPath("$[0].products[0].category").value("ADITIVO"));

        verify(sellerCloseQueryService).registerVendorCloses(eq(date), anyList());
        verify(sellerCloseQueryService).findVendorClosesByDate(date);
    }

    @Test
    @DisplayName("POST /vendor-closes returns 400 VALIDATION_ERROR when nested fields are blank or negative")
    void registerVendorCloses_returns400_whenNestedValidationFails() throws Exception {
        String invalidBody = """
                {
                  "journalDate": "2026-11-19",
                  "closes": [
                    {
                      "vendorDocument": "",
                      "vendorName": "Mónica Dorado",
                      "shiftName": "12-7",
                      "pumpNumber": 3,
                      "cash": -5000,
                      "ga": 0,
                      "credibanco": 0,
                      "qr": 0,
                      "redemptions": 0,
                      "totalDelivered": 0,
                      "gallonsMotor": 0,
                      "gallonsDiesel": 0,
                      "gallonsExtra": 0,
                      "gallonsMaxpro": 0,
                      "basketDominusAmount": 0,
                      "dominusTirilla": 0,
                      "products": []
                    }
                  ]
                }
                """;

        mockMvc.perform(post(BASE)
                        .param("date", "2026-11-19")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("closes[0].vendorDocument")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("closes[0].cash")));

        verifyNoInteractions(sellerCloseQueryService);
    }

    @Test
    @DisplayName("DELETE /vendor-closes returns 204 when journal is open")
    void resetVendorCloses_returns204_whenOpen() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 19);
        doNothing().when(sellerCloseQueryService).resetVendorCloses(date);

        mockMvc.perform(delete(BASE)
                        .param("date", "2026-11-19")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(sellerCloseQueryService).resetVendorCloses(date);
    }

    @Test
    @DisplayName("DELETE /vendor-closes returns 409 JOURNAL_STATE_CONFLICT when journal is closed")
    void resetVendorCloses_returns409_whenClosed() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 5);
        doThrow(new IllegalStateException("No se pueden restablecer cierres en una jornada cerrada"))
                .when(sellerCloseQueryService).resetVendorCloses(date);

        mockMvc.perform(delete(BASE)
                        .param("date", "2026-08-05")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOURNAL_STATE_CONFLICT"));

        verify(sellerCloseQueryService).resetVendorCloses(date);
    }

    @Test
    @DisplayName("Anonymous requests are denied by Spring Security chain")
    void anonymousRequestsAreDenied() throws Exception {
        mockMvc.perform(get(BASE + "/fuel-prices").param("date", "2026-11-19"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sellerCloseQueryService);
    }
}
