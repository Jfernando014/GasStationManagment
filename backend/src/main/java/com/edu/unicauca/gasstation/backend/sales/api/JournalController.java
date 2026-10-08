package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalStepResponse;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    @PostMapping("/open")
    public ResponseEntity<JournalResponse> getOrCreateJournal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Journal journal = journalService.getOrCreateJournal(date);
        return ResponseEntity.ok(mapToResponse(journal));
    }

    @PostMapping("/{journalId}/steps/{stepNumber}/complete")
    public ResponseEntity<JournalStepResponse> completeStep(
            @PathVariable Long journalId,
            @PathVariable Integer stepNumber) {
        JournalStep step = journalStepService.markStepAsCompleted(journalId, stepNumber);
        return ResponseEntity.ok(JournalStepResponse.builder()
                .id(step.getId())
                .stepNumber(step.getStepNumber())
                .name(step.getName())
                .status(step.getStatus())
                .completedAt(step.getCompletedAt())
                .build());
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
