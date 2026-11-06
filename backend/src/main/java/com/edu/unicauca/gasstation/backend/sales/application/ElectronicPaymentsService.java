package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicPaymentsStatus;
import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicPaymentsStatus.Blocker;
import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicVendorRow;
import com.edu.unicauca.gasstation.backend.sales.api.dto.ElectronicVoucherRequest;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.CredibancoDayResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PointsResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PointsStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.QrReconciliationResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationValidationException;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.VendorDeclaration;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ElectronicPaymentsService {
    private final CredibancoReconciliationService credibancoService;
    private final QrPointsReconciliationService qrPointsService;
    private final CardReconciliationRepository cards;
    private final SellerCloseQuery sellerCloseQuery;
    private final JournalRepository journals;

    @Transactional
    public ElectronicPaymentsStatus getStatus(LocalDate date) {
        Journal journal = journals.findByDate(date).orElse(null);
        List<VendorCloseSummary> closes = sellerCloseQuery.findVendorClosesByDate(date);
        if (isStepCompleted(journal, 2)) {
            synchronizePlanillas(date, closes);
        }

        List<VendorDeclaration> declarations = cards.findByDate(date);
        Map<UUID, VendorDeclaration> declarationsByWorker = new HashMap<>();
        declarations.forEach(declaration -> declarationsByWorker.put(declaration.workerId(), declaration));
        List<ElectronicVendorRow> vendors = closes.stream()
                .map(close -> toRow(close, declarationsByWorker.get(workerId(close))))
                .toList();

        CredibancoDayResult credibanco = credibancoService.get(date);
        QrReconciliationResult qr = qrPointsService.qr(date);
        PointsResult points = qrPointsService.points(date);
        List<Blocker> blockers = blockers(journal, closes, credibanco, qr, points);
        return new ElectronicPaymentsStatus(date, blockers.isEmpty(), blockers, vendors, credibanco, qr, points);
    }

    @Transactional
    public ElectronicPaymentsStatus saveVouchers(LocalDate date, UUID workerId, ElectronicVoucherRequest request) {
        List<VendorCloseSummary> closes = sellerCloseQuery.findVendorClosesByDate(date);
        VendorCloseSummary close = closes.stream()
                .filter(item -> workerId(item).equals(workerId))
                .findFirst()
                .orElseThrow(() -> new ReconciliationValidationException(
                        "workerId", "El vendedor no tiene cierre en el paso 2 para " + date));

        VendorDeclaration declaration = new VendorDeclaration(
                workerId,
                close.shiftName(),
                request.mastercard(),
                request.visa(),
                request.amex(),
                request.mastercardDawn(),
                request.visaDawn(),
                request.amexDawn(),
                normalized(close.credibanco()),
                normalized(close.qr()),
                normalized(close.redemptions()));

        credibancoService.saveVendor(date, declaration);
        return getStatus(date);
    }

    private void synchronizePlanillas(LocalDate date, List<VendorCloseSummary> closes) {
        // The existing QR and Credibanco calculators read planillas from card declarations.
        // Seed/synchronize those rows from step 2 while preserving already-entered vouchers.
        Map<UUID, VendorDeclaration> existing = new HashMap<>();
        cards.findByDate(date).forEach(declaration -> existing.put(declaration.workerId(), declaration));
        for (VendorCloseSummary close : closes) {
            UUID workerId = workerId(close);
            VendorDeclaration previous = existing.get(workerId);
            VendorDeclaration synchronizedDeclaration = new VendorDeclaration(
                    workerId,
                    close.shiftName(),
                    previous == null ? BigDecimal.ZERO : previous.mastercard(),
                    previous == null ? BigDecimal.ZERO : previous.visa(),
                    previous == null ? BigDecimal.ZERO : previous.amex(),
                    previous == null ? BigDecimal.ZERO : previous.mastercardDawn(),
                    previous == null ? BigDecimal.ZERO : previous.visaDawn(),
                    previous == null ? BigDecimal.ZERO : previous.amexDawn(),
                    normalized(close.credibanco()),
                    normalized(close.qr()),
                    normalized(close.redemptions()));
            if (!synchronizedDeclaration.equals(previous)) {
                cards.upsert(date, synchronizedDeclaration);
            }
        }
    }

    private ElectronicVendorRow toRow(VendorCloseSummary close, VendorDeclaration declaration) {
        return new ElectronicVendorRow(
                workerId(close),
                close.vendorName(),
                close.shiftName(),
                amount(declaration, VendorDeclaration::mastercard),
                amount(declaration, VendorDeclaration::visa),
                amount(declaration, VendorDeclaration::amex),
                amount(declaration, VendorDeclaration::mastercardDawn),
                amount(declaration, VendorDeclaration::visaDawn),
                amount(declaration, VendorDeclaration::amexDawn),
                normalized(close.credibanco()),
                normalized(close.qr()),
                normalized(close.redemptions()));
    }

    private List<Blocker> blockers(Journal journal, List<VendorCloseSummary> closes,
                                   CredibancoDayResult credibanco, QrReconciliationResult qr, PointsResult points) {
        List<Blocker> blockers = new ArrayList<>();
        if (journal == null) {
            blockers.add(new Blocker("JOURNAL_NOT_FOUND", "No existe una jornada para esta fecha."));
        } else {
            if (journal.getStatus() != JournalStatus.OPEN || isStepCompleted(journal, 6)) {
                blockers.add(new Blocker("JOURNAL_CLOSED", "La jornada ya está cerrada."));
            }
            if (!isStepCompleted(journal, 2)) {
                blockers.add(new Blocker("STEP2_INCOMPLETE", "Completa primero el paso 2: cierres de vendedores."));
            }
        }
        if (closes.isEmpty()) {
            blockers.add(new Blocker("NO_VENDORS", "No hay vendedores registrados en el paso 2."));
        }
        if (!credibanco.allMatch()) {
            blockers.add(new Blocker("CREDIBANCO_MISMATCH", "Hay vendedores cuyos comprobantes no cuadran con Credibanco."));
        }
        if (!qr.redebanLoaded()) {
            if (qr.planillaTotal().signum() > 0) {
                blockers.add(new Blocker("QR_FILE_REQUIRED", "Carga el CSV de Redeban para cuadrar el QR."));
            }
        } else if (!qr.matchesDay() || !qr.perVendorReliable()
                || qr.vendors().stream().anyMatch(vendor -> !vendor.matches())) {
            blockers.add(new Blocker("QR_MISMATCH", "El QR de Redeban no cuadra por día o por vendedor."));
        }
        if (points.status() == PointsStatus.PENDING_TOTAL) {
            blockers.add(new Blocker("POINTS_TOTAL_REQUIRED", "Registra el total de Puntos Colombia."));
        } else if (points.status() != PointsStatus.COMPLETED_ZERO && points.status() != PointsStatus.MATCH) {
            blockers.add(new Blocker("POINTS_MISMATCH", "El total de Puntos Colombia no cuadra con las planillas."));
        }
        return blockers;
    }

    private UUID workerId(VendorCloseSummary close) {
        try {
            if (close.vendorDocument() == null || close.vendorDocument().isBlank()) {
                throw new IllegalArgumentException("El identificador está vacío");
            }
            return UUID.fromString(close.vendorDocument());
        } catch (IllegalArgumentException exception) {
            throw new ReconciliationValidationException(
                    "vendorDocument", "El identificador del vendedor en el paso 2 no es un UUID válido.");
        }
    }

    private BigDecimal amount(VendorDeclaration declaration,
                              java.util.function.Function<VendorDeclaration, BigDecimal> field) {
        return declaration == null ? BigDecimal.ZERO.setScale(2) : field.apply(declaration);
    }

    private BigDecimal normalized(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private boolean isStepCompleted(Journal journal, int number) {
        return journal != null && journal.getSteps().stream()
                .anyMatch(step -> step.getStepNumber() == number && step.getStatus() == StepStatus.COMPLETED);
    }
}
