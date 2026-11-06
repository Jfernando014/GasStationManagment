package com.edu.unicauca.gasstation.backend.sales.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicVoucherRequest;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.CredibancoDayResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.DayFranchiseTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PointsResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PointsStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.QrReconciliationResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.VendorCredibancoCheck;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.VendorDeclaration;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElectronicPaymentsServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final UUID WORKER_ID = UUID.fromString("6a7cd310-8814-4f4a-9c64-13fb16711af1");

    @Mock
    private CredibancoReconciliationService credibancoService;
    @Mock
    private QrPointsReconciliationService qrPointsService;
    @Mock
    private CardReconciliationRepository cards;
    @Mock
    private SellerCloseQuery sellerCloseQuery;
    @Mock
    private JournalRepository journals;
    @InjectMocks
    private ElectronicPaymentsService service;

    private VendorDeclaration stored;

    @BeforeEach
    void setUp() {
        stored = new VendorDeclaration(WORKER_ID, "DIA",
                money(500), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                money(50), money(20), money(3));
        when(journals.findByDate(DATE)).thenReturn(Optional.of(openJournalWithStep2Complete()));
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of(vendorClose()));
        when(cards.findByDate(DATE)).thenReturn(List.of(stored));
        when(credibancoService.get(DATE)).thenReturn(credibancoResult());
        when(qrPointsService.qr(DATE)).thenReturn(qrResult());
        when(qrPointsService.points(DATE)).thenReturn(pointsResult());
    }

    @Test
    void synchronizesPlanillaFromStepTwoWithoutOverwritingVoucherAmounts() {
        var status = service.getStatus(DATE);

        assertTrue(status.canComplete());
        assertTrue(status.blockers().isEmpty());
        assertEquals(1, status.vendors().size());
        assertEquals(money(500), status.vendors().get(0).mastercard());
        assertEquals(money(100), status.vendors().get(0).planillaCredibanco());
        assertEquals(money(0), status.vendors().get(0).planillaQr());

        ArgumentCaptor<VendorDeclaration> saved = ArgumentCaptor.forClass(VendorDeclaration.class);
        verify(cards).upsert(eq(DATE), saved.capture());
        assertEquals(money(500), saved.getValue().mastercard());
        assertEquals(money(100), saved.getValue().planillaCredibanco());
        assertEquals(money(0), saved.getValue().planillaQr());
        assertEquals(money(0), saved.getValue().planillaRedemptions());
    }

    @Test
    void savesOnlyVoucherAmountsAndUsesPlanillaValuesFromStepTwo() {
        var request = new ElectronicVoucherRequest(
                money(600), money(20), money(10), money(5), money(3), money(2));

        service.saveVouchers(DATE, WORKER_ID, request);

        ArgumentCaptor<VendorDeclaration> saved = ArgumentCaptor.forClass(VendorDeclaration.class);
        verify(credibancoService).saveVendor(eq(DATE), saved.capture());
        assertEquals(money(600), saved.getValue().mastercard());
        assertEquals(money(20), saved.getValue().visa());
        assertEquals(money(10), saved.getValue().amex());
        assertEquals(money(100), saved.getValue().planillaCredibanco());
        assertEquals(money(0), saved.getValue().planillaQr());
        assertEquals(money(0), saved.getValue().planillaRedemptions());
    }

    private Journal openJournalWithStep2Complete() {
        JournalStep step2 = JournalStep.builder().stepNumber(2).status(StepStatus.COMPLETED).build();
        return Journal.builder()
                .date(DATE)
                .status(JournalStatus.OPEN)
                .steps(List.of(step2))
                .build();
    }

    private VendorCloseSummary vendorClose() {
        return new VendorCloseSummary(DATE, WORKER_ID.toString(), "Vendedor", "DIA", 1,
                money(0), money(0), money(100), money(0), money(0), money(100),
                money(0), money(0), money(0), money(0), money(0), money(0), money(0), "CUADRA", List.of());
    }

    private CredibancoDayResult credibancoResult() {
        var check = new VendorCredibancoCheck(WORKER_ID, "Vendedor", "DIA",
                money(500), money(100), money(0), true);
        return new CredibancoDayResult(DATE, List.of(check), true, DayFranchiseTotals.ZERO,
                false, money(0), null, null, null, money(0), money(0), money(0));
    }

    private QrReconciliationResult qrResult() {
        return new QrReconciliationResult(DATE, false, money(0), money(0), money(0), money(0),
                false, money(0), false, Map.of(), List.of(), List.of());
    }

    private PointsResult pointsResult() {
        return new PointsResult(DATE, money(0), null, null, PointsStatus.COMPLETED_ZERO, null, List.of());
    }

    private static BigDecimal money(long value) {
        return BigDecimal.valueOf(value).setScale(2);
    }
}
