package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.application.SalesParameterService;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Domain service responsible for Step 3 of the daily journal:
 * "Apertura de surtidores" / commission reconciliation.
 *
 * <p>What it does:
 * <ol>
 *   <li>Retrieves the consolidated day summary (Step 2 output) via {@link SellerCloseQuery}.</li>
 *   <li>Fetches the effective Credibanco commission rate from {@link SalesParameterService}.</li>
 *   <li>Computes the expected commission amount: {@code totalCredibanco × commissionRate}.</li>
 *   <li>Verifies that the declared totals are present and coherent (Credibanco amount must be positive
 *       when closes exist).</li>
 *   <li>Marks Step 3 as COMPLETED via {@link JournalStepService} when everything reconciles.</li>
 * </ol>
 *
 * <p>Step 3 is considered complete when:
 * <ul>
 *   <li>At least one vendor close exists for the journal date.</li>
 *   <li>The Credibanco amount declared in Step 2 is non-negative.</li>
 *   <li>A valid Credibanco commission rate parameter is available for the journal date.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalStep3ValidationService {

    static final int STEP_NUMBER = 3;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final JournalRepository journalRepository;
    private final SellerCloseQuery sellerCloseQuery;
    private final SalesParameterService salesParameterService;
    private final JournalStepService journalStepService;

    /**
     * Validates Step 3 for the given journal and, if everything reconciles, marks it as completed.
     *
     * @param journalId the ID of the journal to validate
     * @return the completed {@link JournalStep} (step 3)
     * @throws JournalNotFoundException     if no journal exists for the given ID
     * @throws IllegalStateException        if Step 2 has no vendor closes or the commission
     *                                      parameter is missing for the journal date
     */
    @Transactional
    public JournalStep validateAndCompleteStep3(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));

        LocalDate journalDate = journal.getDate();

        // 1. Retrieve consolidated day summary from Step 2
        DaySummary daySummary = sellerCloseQuery.findDaySummary(journalDate)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay cierres de vendedores registrados para la fecha " + journalDate
                                + ". Debe completar el paso 2 antes de validar el paso 3."));

        // 2. Fetch effective Credibanco commission rate from parameters
        BigDecimal commissionRate = salesParameterService
                .effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, journalDate)
                .value();

        // 3. Compute expected commission amount
        BigDecimal totalCredibanco = daySummary.totalCredibanco() != null
                ? daySummary.totalCredibanco()
                : BigDecimal.ZERO;

        BigDecimal expectedCommission = totalCredibanco
                .multiply(commissionRate)
                .setScale(MONEY_SCALE, ROUNDING);

        log.info("[Step3] journalId={} date={} totalCredibanco={} commissionRate={} expectedCommission={}",
                journalId, journalDate, totalCredibanco, commissionRate, expectedCommission);

        // 4. Verify declared Credibanco amount is coherent (non-negative)
        if (totalCredibanco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    "El total Credibanco declarado en el paso 2 es negativo (" + totalCredibanco
                            + "). No se puede completar el paso 3.");
        }

        // 5. Mark Step 3 as completed
        JournalStep completedStep = journalStepService.markStepAsCompleted(journalId, STEP_NUMBER);

        log.info("[Step3] Paso 3 completado para journalId={} date={} | comisión Credibanco aplicada={}%",
                journalId, journalDate, commissionRate.multiply(BigDecimal.valueOf(100)));

        return completedStep;
    }

    /**
     * Returns the reconciliation summary for Step 3 without persisting any changes.
     * Useful for preview / frontend display before the user confirms.
     *
     * @param journalId the ID of the journal
     * @return a {@link Step3ReconciliationResult} with the computed commission details
     */
    @Transactional(readOnly = true)
    public Step3ReconciliationResult previewReconciliation(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));

        LocalDate journalDate = journal.getDate();

        DaySummary daySummary = sellerCloseQuery.findDaySummary(journalDate)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay cierres de vendedores para la fecha " + journalDate));

        BigDecimal commissionRate = salesParameterService
                .effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, journalDate)
                .value();

        BigDecimal totalCredibanco = daySummary.totalCredibanco() != null
                ? daySummary.totalCredibanco()
                : BigDecimal.ZERO;

        BigDecimal expectedCommission = totalCredibanco
                .multiply(commissionRate)
                .setScale(MONEY_SCALE, ROUNDING);

        boolean reconciles = totalCredibanco.compareTo(BigDecimal.ZERO) >= 0;

        return new Step3ReconciliationResult(
                journalId,
                journalDate,
                totalCredibanco,
                commissionRate,
                expectedCommission,
                daySummary.totalDelivered(),
                reconciles
        );
    }

    /**
     * Read-only projection of the Step 3 commission reconciliation result.
     *
     * @param journalId          the journal ID
     * @param journalDate        the operational date of the journal
     * @param totalCredibanco    total Credibanco amount declared in Step 2
     * @param commissionRate     Credibanco commission rate in effect on {@code journalDate}
     * @param expectedCommission computed commission: {@code totalCredibanco × commissionRate}
     * @param totalDelivered     grand total delivered declared in Step 2
     * @param reconciles         {@code true} when Step 3 can be safely completed
     */
    public record Step3ReconciliationResult(
            Long journalId,
            LocalDate journalDate,
            BigDecimal totalCredibanco,
            BigDecimal commissionRate,
            BigDecimal expectedCommission,
            BigDecimal totalDelivered,
            boolean reconciles
    ) {}
}
