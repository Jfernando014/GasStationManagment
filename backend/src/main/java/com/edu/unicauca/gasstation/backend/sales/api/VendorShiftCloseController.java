package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.api.dto.VendorShiftCloseBatchRequestDto;
import com.edu.unicauca.gasstation.backend.sales.application.SellerCloseQueryServiceImpl;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vendor Shift Closes", description = "Individual seller shifts closing, reconciliations, and daily totals (Step 2 of daily journal)")
@RestController
@RequestMapping("/api/v1/sales/vendor-closes")
@RequiredArgsConstructor
public class VendorShiftCloseController {

    private final SellerCloseQueryServiceImpl sellerCloseQueryService;

    @Operation(summary = "Get vendor closes for an operational journal date")
    @ApiResponse(responseCode = "200", description = "Vendor closes list returned")
    @GetMapping
    public List<VendorCloseSummary> getVendorCloses(
            @Parameter(description = "Operational journal date (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate journalDate = date != null ? date : LocalDate.now();
        return sellerCloseQueryService.findVendorClosesByDate(journalDate);
    }

    @Operation(summary = "Get consolidated day sales summary for an operational journal date")
    @ApiResponse(responseCode = "200", description = "Day summary returned")
    @ApiResponse(responseCode = "404", description = "No closes found for date")
    @GetMapping("/day-summary")
    public ResponseEntity<DaySummary> getDaySummary(
            @Parameter(description = "Operational journal date (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate journalDate = date != null ? date : LocalDate.now();
        return sellerCloseQueryService.findDaySummary(journalDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get assigned staff and turnos for an operational journal date")
    @ApiResponse(responseCode = "200", description = "Assigned staff list returned")
    @GetMapping("/assigned-staff")
    public List<AssignedVendor> getAssignedStaff(
            @Parameter(description = "Operational journal date (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate journalDate = date != null ? date : LocalDate.now();
        return sellerCloseQueryService.getAssignedStaffForDate(journalDate);
    }

    @Operation(summary = "Register and reconcile vendor shift closes for a journal date")
    @ApiResponse(responseCode = "200", description = "Vendor closes registered and evaluated")
    @PostMapping
    public List<VendorCloseSummary> registerVendorCloses(
            @Parameter(description = "Operational journal date (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody VendorShiftCloseBatchRequestDto request) {
        LocalDate journalDate = date != null ? date : (request.journalDate() != null ? request.journalDate() : LocalDate.now());
        List<VendorShiftClose> domainCloses = request.closes().stream()
                .map(dto -> dto.toDomain(journalDate))
                .toList();

        sellerCloseQueryService.registerVendorCloses(journalDate, domainCloses);
        return sellerCloseQueryService.findVendorClosesByDate(journalDate);
    }
}
