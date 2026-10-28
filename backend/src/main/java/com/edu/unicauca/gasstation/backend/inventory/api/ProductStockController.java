package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InitialStockRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductStockResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.SalesSyncResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockAdjustmentRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockEntryRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockMovementResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.StockTransferRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.ProductStockService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.ProductStockMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/product-stock")
@RequiredArgsConstructor
@Tag(name = "Product stock", description = "Stock of lubricants, additives and other products in the office and the counter")
public class ProductStockController {

    private final ProductStockService stockService;
    private final ProductStockMapper mapper;

    @PostMapping("/initial")
    @Operation(summary = "Register the opening stock of a product",
            description = "Office and counter are saved together or not at all. It can be done only once per product.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Two movements, one per location"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "The opening stock was already registered")
    })
    public ResponseEntity<List<StockMovementResponse>> registerInitial(
            @Valid @RequestBody InitialStockRequest request) {
        var movements = stockService.registerInitial(request.getProductId(),
                request.getOfficeQuantity().intValueExact(), request.getCounterQuantity().intValueExact(),
                request.getJournalDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toMovementResponses(movements));
    }

    @PostMapping("/entries")
    @Operation(summary = "Register units that arrive at the office")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entry registered"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<StockMovementResponse> registerEntry(@Valid @RequestBody StockEntryRequest request) {
        var movement = stockService.registerEntry(request.getProductId(),
                request.getQuantity().intValueExact(), request.getJournalDate(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toMovementResponse(movement));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Move units from the office to the counter",
            description = "Returns the two ledger lines: negative in the office, positive in the counter.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer registered"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "The office does not have enough units")
    })
    public ResponseEntity<List<StockMovementResponse>> registerTransfer(
            @Valid @RequestBody StockTransferRequest request) {
        var movements = stockService.registerTransfer(request.getProductId(),
                request.getQuantity().intValueExact(), request.getJournalDate(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toMovementResponses(movements));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Correct the stock of a product in a location",
            description = "The quantity is signed and cannot be zero. A reason is mandatory.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adjustment registered"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "The adjustment would leave the stock below zero")
    })
    public ResponseEntity<StockMovementResponse> registerAdjustment(
            @Valid @RequestBody StockAdjustmentRequest request) {
        var movement = stockService.registerAdjustment(request.getProductId(), request.getLocation(),
                request.getQuantity().intValueExact(), request.getJournalDate(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toMovementResponse(movement));
    }

    @PutMapping("/sales/{journalDate}")
    @Operation(summary = "Synchronize the units sold in step 2",
            description = "Replaces the sales movements of the date, so it can be repeated if the sales are corrected. "
                    + "Sold codes that are not in the catalog are listed in the response.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales synchronized"),
            @ApiResponse(responseCode = "400", description = "Invalid journal date"),
            @ApiResponse(responseCode = "409", description = "There are no sales for the journal date")
    })
    public SalesSyncResponse syncSales(
            @Parameter(description = "Journal date (yyyy-MM-dd)", example = "2026-08-05")
            @PathVariable LocalDate journalDate) {
        return mapper.toSyncResponse(stockService.syncSales(journalDate));
    }

    @GetMapping
    @Operation(summary = "Get the stock of the products on a journal date",
            description = "Opening, entries, transfers, adjustments, sales and closing per product and location.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock per product (empty list if there are none)"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameter")
    })
    public List<ProductStockResponse> findByJournalDate(
            @Parameter(description = "Journal date (yyyy-MM-dd)", example = "2026-08-05")
            @RequestParam LocalDate journalDate,
            @Parameter(description = "Optional category filter")
            @RequestParam(required = false) ProductCategory category) {
        return mapper.toProductStockResponses(stockService.findByJournalDate(journalDate, category));
    }
}