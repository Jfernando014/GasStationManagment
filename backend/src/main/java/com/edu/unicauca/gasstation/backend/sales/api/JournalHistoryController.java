package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryCalendar;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryDayDetail;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@Tag(name = "Journal History", description = "Monthly journal history calendar and migrated historical day details (C-10, S3-06)")
@RestController
@RequestMapping("/api/v1/journals/history")
@RequiredArgsConstructor
public class JournalHistoryController {

    private final JournalHistoryService journalHistoryService;

    @Operation(summary = "Get monthly journal history calendar with status, discrepancy and alert markers (C-10, S3-06)")
    @ApiResponse(responseCode = "200", description = "Monthly calendar returned")
    @GetMapping
    public ResponseEntity<JournalHistoryCalendar> getMonthCalendar(
            @Parameter(description = "Target year and month in YYYY-MM format (defaults to 2026-08)")
            @RequestParam(required = false) String yearMonth) {
        YearMonth parsedMonth = (yearMonth != null && !yearMonth.isBlank())
                ? YearMonth.parse(yearMonth.strip())
                : YearMonth.of(2026, 8);
        return ResponseEntity.ok(journalHistoryService.getMonthCalendar(parsedMonth));
    }

    @Operation(summary = "Get historical or migrated day detail for a specific date (C-10, S3-06)")
    @ApiResponse(responseCode = "200", description = "Day detail returned")
    @GetMapping("/{date}")
    public ResponseEntity<JournalHistoryDayDetail> getDayDetail(
            @Parameter(description = "Operational date in YYYY-MM-DD format")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(journalHistoryService.getDayDetail(date));
    }
}
