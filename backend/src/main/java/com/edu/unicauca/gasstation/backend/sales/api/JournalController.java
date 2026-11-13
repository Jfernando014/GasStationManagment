package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalClosingSummaryResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalStepResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.Step3ReconciliationResponse;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStep3ValidationService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStep3ValidationService.Step3ReconciliationResult;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Journals", description = "Operational daily journal lifecycle, step completion, and Step 6 closing (C-02, C-08)")
@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {

    private final JournalService journalService;
    private final JournalStepService journalStepService;
    private final JournalStep3ValidationService journalStep3ValidationService;
    private final JournalClosingSummaryService journalClosingSummaryService;
    private final CashBalanceStepService cashBalanceStepService;

    public JournalController(
            JournalService journalService,
            JournalStepService journalStepService,
            JournalStep3ValidationService journalStep3ValidationService,
            JournalClosingSummaryService journalClosingSummaryService,
            CashBalanceStepService cashBalanceStepService) {
        this.journalService = journalService;
        this.journalStepService = journalStepService;
        this.journalStep3ValidationService = journalStep3ValidationService;
        this.journalClosingSummaryService = journalClosingSummaryService;
        this.cashBalanceStepService = cashBalanceStepService;
    }

    @Operation(summary = "Open or retrieve an operational daily journal by date")
    @ApiResponse(responseCode = "200", description = "Journal opened or retrieved")
    @PostMapping("/open")
    public ResponseEntity<JournalResponse> getOrCreateJournal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Journal journal = journalService.getOrCreateJournal(date);
        return ResponseEntity.ok(mapToResponse(journal));
    }

    @Operation(summary = "Mark a journal step as completed")
    @ApiResponse(responseCode = "200", description = "Step marked as completed")
    @ApiResponse(responseCode = "409", description = "Journal is closed or prerequisites are not met")
    @PostMapping("/{journalId}/steps/{stepNumber}/complete")
    public ResponseEntity<JournalStepResponse> completeStep(
            @PathVariable Long journalId,
            @PathVariable Integer stepNumber,
            Authentication authentication) {
        JournalStep step;
        if (stepNumber == 4) {
            SalesAdminAuthorization.requireAdmin(authentication);
            step = cashBalanceStepService.complete(journalId);
        } else {
            step = journalStepService.markStepAsCompleted(journalId, stepNumber);
        }
        return ResponseEntity.ok(JournalStepResponse.builder()
                .id(step.getId())
                .stepNumber(step.getStepNumber())
                .name(step.getName())
                .status(step.getStatus())
                .completedAt(step.getCompletedAt())
                .build());
    }

    /**
     * GET /api/v1/journals/{journalId}/steps/3/reconciliation
     *
     * <p>Returns the Step 3 commission reconciliation preview without persisting any change.
     * Use this to show the user the Credibanco commission breakdown before confirming.
     */
    @Operation(summary = "Preview Step 3 commission reconciliation")
    @GetMapping("/{journalId}/steps/3/reconciliation")
    public ResponseEntity<Step3ReconciliationResponse> previewStep3Reconciliation(
            @PathVariable Long journalId) {
        Step3ReconciliationResult result = journalStep3ValidationService.previewReconciliation(journalId);
        return ResponseEntity.ok(toStep3Response(result, false));
    }

    /**
     * POST /api/v1/journals/{journalId}/steps/3/complete
     *
     * <p>Validates Step 3 (Credibanco commission reconciliation against declared vendor closes)
     * and marks it as COMPLETED when everything reconciles.
     */
    @Operation(summary = "Validate and complete Step 3 commission reconciliation")
    @PostMapping("/{journalId}/steps/3/complete")
    public ResponseEntity<Step3ReconciliationResponse> completeStep3(
            @PathVariable Long journalId) {
        JournalStep completedStep = journalStep3ValidationService.validateAndCompleteStep3(journalId);
        Step3ReconciliationResult preview = journalStep3ValidationService.previewReconciliation(journalId);
        return ResponseEntity.ok(toStep3Response(preview, completedStep != null));
    }

    /**
     * GET /api/v1/journals/{journalId}/closing-summary
     *
     * <p>Returns the Step 6 closing summary (steps 1..5 status, discrepancies, fuel variations, and new alerts).
     */
    @Operation(summary = "Get Step 6 closing summary including variations and new inventory alerts (C-08, S3-05)")
    @ApiResponse(responseCode = "200", description = "Closing summary returned")
    @ApiResponse(responseCode = "404", description = "Journal not found")
    @GetMapping("/{journalId}/closing-summary")
    public ResponseEntity<JournalClosingSummaryResponse> getClosingSummary(@PathVariable Long journalId) {
        var summary = journalClosingSummaryService.getClosingSummary(journalId);
        return ResponseEntity.ok(JournalClosingSummaryResponse.from(summary));
    }

    /**
     * POST /api/v1/journals/{journalId}/close
     *
     * <p>Closes the journal (Step 6, C-08), locking all steps in read-only mode and publishing
     * {@code JournalClosedEvent} (S3-03, S3-04).
     */
    @Operation(summary = "Close the operational journal, lock it in read-only mode, and emit JournalClosedEvent (C-08)")
    @ApiResponse(responseCode = "200", description = "Journal closed")
    @ApiResponse(responseCode = "404", description = "Journal not found")
    @ApiResponse(responseCode = "409", description = "Journal is already closed")
    @PostMapping("/{journalId}/close")
    public ResponseEntity<JournalResponse> closeJournal(@PathVariable Long journalId) {
        Journal closed = journalService.closeJournal(journalId);
        return ResponseEntity.ok(mapToResponse(closed));
    }

    private Step3ReconciliationResponse toStep3Response(Step3ReconciliationResult result, boolean stepCompleted) {
        return new Step3ReconciliationResponse(
                result.journalId(),
                result.journalDate(),
                result.totalCredibanco(),
                result.commissionRate(),
                result.expectedCommission(),
                result.totalDelivered(),
                result.reconciles(),
                stepCompleted
        );
    }

    private JournalResponse mapToResponse(Journal journal) {
        List<JournalStep> steps = journal.getSteps() != null ? journal.getSteps() : List.of();
        LocalDateTime closedAt = steps.stream()
                .filter(step -> step.getStepNumber() != null && step.getStepNumber() == 6)
                .map(JournalStep::getCompletedAt)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);

        return JournalResponse.builder()
                .id(journal.getId())
                .date(journal.getDate())
                .consecutive(journal.getConsecutive())
                .status(journal.getStatus())
                .closedAt(closedAt)
                .steps(steps.stream()
                        .map(step -> JournalStepResponse.builder()
                                .id(step.getId())
                                .stepNumber(step.getStepNumber())
                                .name(step.getName())
                                .status(step.getStatus())
                                .completedAt(step.getCompletedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
