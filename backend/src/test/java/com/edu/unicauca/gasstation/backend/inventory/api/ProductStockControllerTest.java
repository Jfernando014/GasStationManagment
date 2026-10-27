package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InitialStockRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockAdjustmentRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockEntryRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockTransferRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.LocationStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.SalesSyncResult;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.ProductStockService;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidStockMovementException;
import com.edu.unicauca.gasstation.backend.inventory.exception.NoSalesDataException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProductNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.StockConflictException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.ProductStockMapperImpl;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductStockController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "server.port=0")
@Import(ProductStockMapperImpl.class)
class ProductStockControllerTest {

    private static final String URL = "/api/v1/inventory/product-stock";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductStockService stockService;

    // ---------- initial stock ----------

    @Test
    void initialStockReturnsCreatedWithOneMovementPerLocation() throws Exception {
        when(stockService.registerInitial(1L, 12, 5, DATE)).thenReturn(List.of(
                movement(1L, StockLocation.OFFICE, "INITIAL", 12), movement(2L, StockLocation.COUNTER, "INITIAL", 5)));

        postJson("/initial", initialRequest("12", "5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].location").value("OFFICE"))
                .andExpect(jsonPath("$[0].movementType").value("INITIAL"))
                .andExpect(jsonPath("$[0].quantity").value(12))
                .andExpect(jsonPath("$[1].location").value("COUNTER"))
                .andExpect(jsonPath("$[1].quantity").value(5));
        verify(stockService).registerInitial(eq(1L), eq(12), eq(5), eq(DATE));
    }

    @Test
    void initialStockAcceptsZero() throws Exception {
        when(stockService.registerInitial(1L, 0, 0, DATE)).thenReturn(List.of());

        postJson("/initial", initialRequest("0", "0")).andExpect(status().isCreated());
    }

    @Test
    void initialStockRejectsNegativeAndDecimalQuantities() throws Exception {
        for (String invalid : List.of("-1", "2.5")) {
            postJson("/initial", initialRequest(invalid, "5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].field").value("officeQuantity"))
                    .andExpect(jsonPath("$.fields[0].message").value(InitialStockRequest.INITIAL_INVALID));
            postJson("/initial", initialRequest("5", invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].field").value("counterQuantity"));
        }
        verifyNoInteractions(stockService);
    }

    @Test
    void initialStockRejectsAMissingQuantityProductOrDate() throws Exception {
        InitialStockRequest noCounter = initialRequest("5", "5");
        noCounter.setCounterQuantity(null);
        postJson("/initial", noCounter)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InitialStockRequest.INITIAL_INVALID));

        InitialStockRequest noProduct = initialRequest("5", "5");
        noProduct.setProductId(null);
        postJson("/initial", noProduct)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InitialStockRequest.PRODUCT_REQUIRED));

        InitialStockRequest noDate = initialRequest("5", "5");
        noDate.setJournalDate(null);
        postJson("/initial", noDate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(InitialStockRequest.DATE_REQUIRED));
        verifyNoInteractions(stockService);
    }

    @Test
    void initialStockRejectsAQuantityThatIsNotANumber() throws Exception {
        postRaw("/initial", "{\"productId\":1,\"officeQuantity\":\"abc\",\"counterQuantity\":5,"
                + "\"journalDate\":\"2026-08-05\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"));
        verifyNoInteractions(stockService);
    }

    @Test
    void initialStockReturnsConflictWhenItWasAlreadyRegistered() throws Exception {
        when(stockService.registerInitial(1L, 12, 5, DATE))
                .thenThrow(new StockConflictException("The initial stock of the product was already registered"));

        postJson("/initial", initialRequest("12", "5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("The initial stock of the product was already registered"));
    }

    @Test
    void initialStockReturnsNotFoundForAnUnknownProduct() throws Exception {
        when(stockService.registerInitial(1L, 12, 5, DATE)).thenThrow(new ProductNotFoundException(1L));

        postJson("/initial", initialRequest("12", "5")).andExpect(status().isNotFound());
    }

    // ---------- entries ----------

    @Test
    void entryReturnsCreatedWithTheMovement() throws Exception {
        when(stockService.registerEntry(1L, 30, DATE, "Invoice 12"))
                .thenReturn(movement(7L, StockLocation.OFFICE, "ENTRY", 30));

        postJson("/entries", entryRequest("30"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.movementType").value("ENTRY"))
                .andExpect(jsonPath("$.quantity").value(30));
        verify(stockService).registerEntry(eq(1L), eq(30), eq(DATE), eq("Invoice 12"));
    }

    @Test
    void entryRejectsInvalidQuantities() throws Exception {
        for (String invalid : List.of("0", "-3")) {
            postJson("/entries", entryRequest(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].message").value(StockEntryRequest.QUANTITY_POSITIVE));
        }
        for (String invalid : List.of("2.5", "1234567")) {
            postJson("/entries", entryRequest(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields[0].message").value(StockEntryRequest.QUANTITY_DIGITS));
        }
        verifyNoInteractions(stockService);
    }

    @Test
    void entryRejectsATooLongNote() throws Exception {
        StockEntryRequest request = entryRequest("30");
        request.setNote("x".repeat(201));

        postJson("/entries", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(StockEntryRequest.NOTE_SIZE));
        verifyNoInteractions(stockService);
    }

    @Test
    void entryReturnsBadRequestWhenTheServiceRejectsIt() throws Exception {
        when(stockService.registerEntry(1L, 30, DATE, "Invoice 12"))
                .thenThrow(new InvalidStockMovementException("The product is inactive"));

        postJson("/entries", entryRequest("30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("The product is inactive"));
    }

    // ---------- transfers ----------

    @Test
    void transferReturnsCreatedWithBothLines() throws Exception {
        when(stockService.registerTransfer(1L, 10, DATE, "Restock")).thenReturn(List.of(
                movement(8L, StockLocation.OFFICE, "TRANSFER", -10),
                movement(9L, StockLocation.COUNTER, "TRANSFER", 10)));

        postJson("/transfers", transferRequest("10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].location").value("OFFICE"))
                .andExpect(jsonPath("$[0].quantity").value(-10))
                .andExpect(jsonPath("$[1].location").value("COUNTER"))
                .andExpect(jsonPath("$[1].quantity").value(10));
    }

    @Test
    void transferRejectsAQuantityThatIsNotPositive() throws Exception {
        postJson("/transfers", transferRequest("0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(StockTransferRequest.QUANTITY_POSITIVE));
        verifyNoInteractions(stockService);
    }

    @Test
    void transferReturnsConflictWhenTheOfficeDoesNotHaveEnoughUnits() throws Exception {
        when(stockService.registerTransfer(1L, 10, DATE, "Restock"))
                .thenThrow(new StockConflictException("The office does not have enough units for the transfer"));

        postJson("/transfers", transferRequest("10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------- adjustments ----------

    @Test
    void adjustmentReturnsCreatedAndAcceptsANegativeQuantity() throws Exception {
        when(stockService.registerAdjustment(1L, StockLocation.COUNTER, -2, DATE, "Damaged"))
                .thenReturn(movement(10L, StockLocation.COUNTER, "ADJUSTMENT", -2));

        postJson("/adjustments", adjustmentRequest("-2", "Damaged"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantity").value(-2));
        verify(stockService).registerAdjustment(eq(1L), eq(StockLocation.COUNTER), eq(-2), eq(DATE), eq("Damaged"));
    }

    @Test
    void adjustmentRequiresAReason() throws Exception {
        postJson("/adjustments", adjustmentRequest("-2", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("reason"))
                .andExpect(jsonPath("$.fields[0].message").value(StockAdjustmentRequest.REASON_REQUIRED));
        verifyNoInteractions(stockService);
    }

    @Test
    void adjustmentRejectsADecimalQuantityAndAMissingLocation() throws Exception {
        postJson("/adjustments", adjustmentRequest("1.5", "Reason"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(StockAdjustmentRequest.QUANTITY_DIGITS));

        StockAdjustmentRequest noLocation = adjustmentRequest("2", "Reason");
        noLocation.setLocation(null);
        postJson("/adjustments", noLocation)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].message").value(StockAdjustmentRequest.LOCATION_REQUIRED));
        verifyNoInteractions(stockService);
    }

    @Test
    void adjustmentRejectsAnUnknownLocation() throws Exception {
        postRaw("/adjustments", "{\"productId\":1,\"location\":\"BASEMENT\",\"quantity\":2,"
                + "\"journalDate\":\"2026-08-05\",\"reason\":\"Reason\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"));
        verifyNoInteractions(stockService);
    }

    @Test
    void adjustmentReturnsConflictWhenItWouldLeaveTheStockBelowZero() throws Exception {
        when(stockService.registerAdjustment(1L, StockLocation.COUNTER, -2, DATE, "Damaged"))
                .thenThrow(new StockConflictException("The adjustment would leave the stock below zero"));

        postJson("/adjustments", adjustmentRequest("-2", "Damaged")).andExpect(status().isConflict());
    }

    // ---------- sales sync ----------

    @Test
    void syncReturnsTheResultWithTheUnmatchedCodes() throws Exception {
        when(stockService.syncSales(DATE)).thenReturn(new SalesSyncResult(DATE, 2, List.of("XYZ")));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/sales/2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalDate").value("2026-08-05"))
                .andExpect(jsonPath("$.syncedProducts").value(2))
                .andExpect(jsonPath("$.unmatchedCodes.length()").value(1))
                .andExpect(jsonPath("$.unmatchedCodes[0]").value("XYZ"));
    }

    @Test
    void syncReturnsConflictWhenThereAreNoSales() throws Exception {
        when(stockService.syncSales(DATE)).thenThrow(new NoSalesDataException(DATE));

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/sales/2026-08-05"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(new NoSalesDataException(DATE).getMessage()));
    }

    @Test
    void syncRejectsAnInvalidDate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/sales/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'journalDate' is invalid"));
        verifyNoInteractions(stockService);
    }

    // ---------- day report ----------

    @Test
    void findReturnsTheStockPerProductAndLocation() throws Exception {
        when(stockService.findByJournalDate(DATE, null)).thenReturn(List.of(productStock()));

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productCode").value("LUB-1"))
                .andExpect(jsonPath("$[0].category").value("LUBRICANTE"))
                .andExpect(jsonPath("$[0].status").value("OK"))
                .andExpect(jsonPath("$[0].office.opening").value(10))
                .andExpect(jsonPath("$[0].office.entries").value(20))
                .andExpect(jsonPath("$[0].office.transfers").value(-6))
                .andExpect(jsonPath("$[0].office.closing").value(24))
                .andExpect(jsonPath("$[0].counter.sold").value(3))
                .andExpect(jsonPath("$[0].counter.closing").value(6));
    }

    @Test
    void findPassesTheCategoryFilter() throws Exception {
        when(stockService.findByJournalDate(DATE, ProductCategory.ADITIVO)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("journalDate", "2026-08-05").param("category", "ADITIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(stockService).findByJournalDate(DATE, ProductCategory.ADITIVO);
    }

    @Test
    void findRequiresTheJournalDate() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The required parameter is missing 'journalDate'"));
        verifyNoInteractions(stockService);
    }

    @Test
    void findRejectsAnUnknownCategory() throws Exception {
        mockMvc.perform(get(URL).param("journalDate", "2026-08-05").param("category", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value of parameter 'category' is invalid"));
        verifyNoInteractions(stockService);
    }

    // ---------- helpers ----------

    private InitialStockRequest initialRequest(String office, String counter) {
        InitialStockRequest request = new InitialStockRequest();
        request.setProductId(1L);
        request.setOfficeQuantity(new BigDecimal(office));
        request.setCounterQuantity(new BigDecimal(counter));
        request.setJournalDate(DATE);
        return request;
    }

    private StockEntryRequest entryRequest(String quantity) {
        StockEntryRequest request = new StockEntryRequest();
        request.setProductId(1L);
        request.setQuantity(new BigDecimal(quantity));
        request.setJournalDate(DATE);
        request.setNote("Invoice 12");
        return request;
    }

    private StockTransferRequest transferRequest(String quantity) {
        StockTransferRequest request = new StockTransferRequest();
        request.setProductId(1L);
        request.setQuantity(new BigDecimal(quantity));
        request.setJournalDate(DATE);
        request.setNote("Restock");
        return request;
    }

    private StockAdjustmentRequest adjustmentRequest(String quantity, String reason) {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setProductId(1L);
        request.setLocation(StockLocation.COUNTER);
        request.setQuantity(new BigDecimal(quantity));
        request.setJournalDate(DATE);
        request.setReason(reason);
        return request;
    }

    private StockMovement movement(Long id, StockLocation location, String type, int quantity) {
        StockMovement movement = new StockMovement();
        movement.setId(id);
        movement.setProductId(1L);
        movement.setJournalDate(DATE);
        movement.setLocation(location);
        movement.setMovementType(com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType
                .valueOf(type));
        movement.setQuantity(quantity);
        movement.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));
        return movement;
    }

    private ProductStock productStock() {
        return ProductStock.builder()
                .productId(1L)
                .productCode("LUB-1")
                .productName("Lubricant 1")
                .category(ProductCategory.LUBRICANTE)
                .active(true)
                .office(LocationStock.builder().opening(10).entries(20).transfers(-6).closing(24).build())
                .counter(LocationStock.builder().opening(4).transfers(6).adjustments(-1).sold(3).closing(6).build())
                .status(StockStatus.OK)
                .build();
    }

    private ResultActions postJson(String path, Object request) throws Exception {
        return postRaw(path, objectMapper.writeValueAsString(request));
    }

    private ResultActions postRaw(String path, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(URL + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}