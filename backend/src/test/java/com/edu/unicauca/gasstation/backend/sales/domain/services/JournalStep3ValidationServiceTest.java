package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.application.SalesParameterService;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStep3ValidationService.Step3ReconciliationResult;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalStep3ValidationService - Credibanco commission reconciliation (Step 3)")
class JournalStep3ValidationServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private SellerCloseQuery sellerCloseQuery;

    @Mock
    private SalesParameterService salesParameterService;

    @Mock
    private JournalStepService journalStepService;

    @InjectMocks
    private JournalStep3ValidationService service;

    private static final Long JOURNAL_ID = 1L;
    private static final LocalDate JOURNAL_DATE = LocalDate.of(2024, 11, 4);
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.0265");

    private Journal journal;

    @BeforeEach
    void setUp() {
        journal = Journal.builder()
                .id(JOURNAL_ID)
                .date(JOURNAL_DATE)
                .consecutive(1L)
                .build();
    }

    // ─── validateAndCompleteStep3 ──────────────────────────────────────────────

    @Test
    @DisplayName("Should complete Step 3 successfully when Credibanco amount is positive and parameter exists")
    void completesStep3WhenCredibancoIsPositiveAndParameterExists() {
        DaySummary daySummary = buildDaySummary(new BigDecimal("1000000.00"), new BigDecimal("5000000.00"));

        JournalStep completedStep = JournalStep.builder()
                .id(30L)
                .stepNumber(3)
                .status(StepStatus.COMPLETED)
                .build();

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(sellerCloseQuery.findDaySummary(JOURNAL_DATE)).thenReturn(Optional.of(daySummary));
        when(salesParameterService.effective(eq(SalesParameterKey.CREDIBANCO_COMMISSION_RATE), eq(JOURNAL_DATE)))
                .thenReturn(new SalesParameterValue(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, COMMISSION_RATE, JOURNAL_DATE));
        when(journalStepService.markStepAsCompleted(JOURNAL_ID, 3)).thenReturn(completedStep);

        JournalStep result = service.validateAndCompleteStep3(JOURNAL_ID);

        assertThat(result.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(result.getStepNumber()).isEqualTo(3);
        verify(journalStepService).markStepAsCompleted(JOURNAL_ID, 3);
    }

    @Test
    @DisplayName("Should complete Step 3 when total Credibanco is zero (no card payments)")
    void completesStep3WhenCredibancoIsZero() {
        DaySummary daySummary = buildDaySummary(BigDecimal.ZERO, new BigDecimal("3000000.00"));

        JournalStep completedStep = JournalStep.builder()
                .id(30L)
                .stepNumber(3)
                .status(StepStatus.COMPLETED)
                .build();

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(sellerCloseQuery.findDaySummary(JOURNAL_DATE)).thenReturn(Optional.of(daySummary));
        when(salesParameterService.effective(eq(SalesParameterKey.CREDIBANCO_COMMISSION_RATE), eq(JOURNAL_DATE)))
                .thenReturn(new SalesParameterValue(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, COMMISSION_RATE, JOURNAL_DATE));
        when(journalStepService.markStepAsCompleted(JOURNAL_ID, 3)).thenReturn(completedStep);

        JournalStep result = service.validateAndCompleteStep3(JOURNAL_ID);

        assertThat(result.getStatus()).isEqualTo(StepStatus.COMPLETED);
        verify(journalStepService).markStepAsCompleted(JOURNAL_ID, 3);
    }

    @Test
    @DisplayName("Should throw JournalNotFoundException when journal does not exist")
    void throwsJournalNotFoundWhenJournalDoesNotExist() {
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndCompleteStep3(JOURNAL_ID))
                .isInstanceOf(JournalNotFoundException.class);

        verifyNoInteractions(sellerCloseQuery, salesParameterService, journalStepService);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no vendor closes exist for the date (Step 2 not done)")
    void throwsIllegalStateWhenNoDaySummaryExists() {
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(sellerCloseQuery.findDaySummary(JOURNAL_DATE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndCompleteStep3(JOURNAL_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay cierres de vendedores");

        verifyNoInteractions(journalStepService);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when total Credibanco is negative")
    void throwsIllegalStateWhenCredibancoIsNegative() {
        DaySummary daySummary = buildDaySummary(new BigDecimal("-100.00"), new BigDecimal("2000000.00"));

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(sellerCloseQuery.findDaySummary(JOURNAL_DATE)).thenReturn(Optional.of(daySummary));
        when(salesParameterService.effective(eq(SalesParameterKey.CREDIBANCO_COMMISSION_RATE), eq(JOURNAL_DATE)))
                .thenReturn(new SalesParameterValue(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, COMMISSION_RATE, JOURNAL_DATE));

        assertThatThrownBy(() -> service.validateAndCompleteStep3(JOURNAL_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("negativo");

        verify(journalStepService, never()).markStepAsCompleted(any(), any());
    }

    // ─── previewReconciliation ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should return correct commission calculation in preview")
    void previewReturnsCorrectCommissionCalculation() {
        BigDecimal totalCredibanco = new BigDecimal("2000000.00");
        DaySummary daySummary = buildDaySummary(totalCredibanco, new BigDecimal("8000000.00"));

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(sellerCloseQuery.findDaySummary(JOURNAL_DATE)).thenReturn(Optional.of(daySummary));
        when(salesParameterService.effective(eq(SalesParameterKey.CREDIBANCO_COMMISSION_RATE), eq(JOURNAL_DATE)))
                .thenReturn(new SalesParameterValue(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, COMMISSION_RATE, JOURNAL_DATE));

        Step3ReconciliationResult result = service.previewReconciliation(JOURNAL_ID);

        // expected commission = 2_000_000 × 0.0265 = 53_000.00
        BigDecimal expectedCommission = new BigDecimal("53000.00");

        assertThat(result.journalId()).isEqualTo(JOURNAL_ID);
        assertThat(result.journalDate()).isEqualTo(JOURNAL_DATE);
        assertThat(result.totalCredibanco()).isEqualByComparingTo(totalCredibanco);
        assertThat(result.commissionRate()).isEqualByComparingTo(COMMISSION_RATE);
        assertThat(result.expectedCommission()).isEqualByComparingTo(expectedCommission);
        assertThat(result.reconciles()).isTrue();
    }

    @Test
    @DisplayName("Should return reconciles=false in preview when Credibanco amount is negative")
    void previewReturnsNotReconcilesWhenCredibancoNegative() {
        DaySummary daySummary = buildDaySummary(new BigDecimal("-50.00"), new BigDecimal("1000000.00"));

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(sellerCloseQuery.findDaySummary(JOURNAL_DATE)).thenReturn(Optional.of(daySummary));
        when(salesParameterService.effective(eq(SalesParameterKey.CREDIBANCO_COMMISSION_RATE), eq(JOURNAL_DATE)))
                .thenReturn(new SalesParameterValue(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, COMMISSION_RATE, JOURNAL_DATE));

        Step3ReconciliationResult result = service.previewReconciliation(JOURNAL_ID);

        assertThat(result.reconciles()).isFalse();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private DaySummary buildDaySummary(BigDecimal totalCredibanco, BigDecimal totalDelivered) {
        return new DaySummary(
                JOURNAL_DATE,
                new BigDecimal("1000000.00"),  // totalCash
                BigDecimal.ZERO,               // totalGa
                totalCredibanco,               // totalCredibanco
                BigDecimal.ZERO,               // totalQr
                BigDecimal.ZERO,               // totalRedemptions
                totalDelivered,                // totalDelivered
                totalDelivered,                // totalDominusTirilla
                BigDecimal.ZERO,               // totalDiscrepancy
                BigDecimal.ZERO,               // gallonsMotor
                BigDecimal.ZERO,               // gallonsDiesel
                BigDecimal.ZERO,               // gallonsExtra
                BigDecimal.ZERO,               // gallonsMaxpro
                BigDecimal.ZERO,               // totalFuelGallons
                BigDecimal.ZERO,               // basketDominusAmount
                0,                             // totalProductUnits
                List.of()                      // products
        );
    }
}
