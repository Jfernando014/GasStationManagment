package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyToleranceConfig;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DiscrepancyToleranceRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidToleranceConfigurationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain service managing vendor discrepancy tolerance policies with effective date.
 * Implements temporal resolution, immutability for closed journals, and history query.
 */
@Service
@RequiredArgsConstructor
public class DiscrepancyToleranceService {

    private final DiscrepancyToleranceRepository toleranceRepository;
    private final JournalRepository journalRepository;

    @Transactional(readOnly = true)
    public BigDecimal getEffectiveTolerance(LocalDate journalDate) {
        final LocalDate targetDate = journalDate != null ? journalDate : LocalDate.now();
        return toleranceRepository.findEffectiveTolerance(targetDate)
                .map(DiscrepancyToleranceConfig::toleranceAmount)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public DiscrepancyToleranceConfig getEffectiveToleranceConfig(LocalDate journalDate) {
        final LocalDate targetDate = journalDate != null ? journalDate : LocalDate.now();
        return toleranceRepository.findEffectiveTolerance(targetDate)
                .orElseGet(() -> DiscrepancyToleranceConfig.create(BigDecimal.ZERO, targetDate, "Default zero baseline", "SYSTEM"));
    }

    @Transactional
    public DiscrepancyToleranceConfig configureTolerance(BigDecimal amount, LocalDate effectiveDate, String notes, String user) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidToleranceConfigurationException("Tolerance amount must be non-negative");
        }
        if (effectiveDate == null) {
            throw new InvalidToleranceConfigurationException("Effective date is mandatory");
        }

        // Rule 3: Do not modify or schedule a version for a date whose journal is already closed
        journalRepository.findByDate(effectiveDate).ifPresent(journal -> {
            if (journal.getStatus() == JournalStatus.CLOSED) {
                throw new InvalidToleranceConfigurationException(
                        "Cannot schedule or modify tolerance for date " + effectiveDate + " because the journal is already closed");
            }
        });

        DiscrepancyToleranceConfig newConfig = DiscrepancyToleranceConfig.create(amount, effectiveDate, notes, user);
        return toleranceRepository.save(newConfig);
    }

    @Transactional(readOnly = true)
    public List<DiscrepancyToleranceConfig> getToleranceHistory() {
        return toleranceRepository.findAllHistory();
    }

    @Transactional(readOnly = true)
    public List<DiscrepancyToleranceConfig> getScheduledTolerances(LocalDate fromDate) {
        LocalDate baseDate = fromDate != null ? fromDate : LocalDate.now();
        return toleranceRepository.findScheduledAfter(baseDate);
    }
}
