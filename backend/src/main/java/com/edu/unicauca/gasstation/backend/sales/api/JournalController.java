package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalStepResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.Step3ReconciliationResponse;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStep3ValidationService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStep3ValidationService.Step3ReconciliationResult;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final JournalStepService journalStepService;
    private final JournalStep3ValidationService journalStep3ValidationService;
    private final CashBalanceStepService cashBalanceStepService;

    @PostMapping("/open")
    public ResponseEntity<JournalResponse> getOrCreateJournal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Journal journal = journalService.getOrCreateJournal(date);
        return ResponseEntity.ok(mapToResponse(journal));
    }

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
    @PostMapping("/{journalId}/steps/3/complete")
    public ResponseEntity<Step3ReconciliationResponse> completeStep3(
            @PathVariable Long journalId) {
        JournalStep completedStep = journalStep3ValidationService.validateAndCompleteStep3(journalId);
        Step3ReconciliationResult preview = journalStep3ValidationService.previewReconciliation(journalId);
        return ResponseEntity.ok(toStep3Response(preview, completedStep != null));
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
        return JournalResponse.builder()
                .id(journal.getId())
                .date(journal.getDate())
                .consecutive(journal.getConsecutive())
                .status(journal.getStatus())
                .steps(journal.getSteps().stream()
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
