package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPriceReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.VendorShiftCloseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.DiscrepancyToleranceService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.VendorCloseCalculationEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation of the public SellerCloseQuery contract for cross-module consumption.
 */
@Service
@Getter 
@Setter 
public class SellerCloseQueryServiceImpl implements SellerCloseQuery {

    private final VendorShiftCloseRepository vendorShiftCloseRepository;
    private final VendorCloseCalculationEngine calculationEngine;
    private final DiscrepancyToleranceService toleranceService;
    private final DayStaffReadPort dayStaffReadPort;
    private final FuelPriceReadPort fuelPriceReadPort;
    private final JournalRepository journalRepository;

    public SellerCloseQueryServiceImpl(
            VendorShiftCloseRepository vendorShiftCloseRepository,
            VendorCloseCalculationEngine calculationEngine,
            DiscrepancyToleranceService toleranceService,
            DayStaffReadPort dayStaffReadPort,
            FuelPriceReadPort fuelPriceReadPort) {
        this(vendorShiftCloseRepository, calculationEngine, toleranceService, dayStaffReadPort, fuelPriceReadPort, null);
    }

    @Autowired
    public SellerCloseQueryServiceImpl(
            VendorShiftCloseRepository vendorShiftCloseRepository,
            VendorCloseCalculationEngine calculationEngine,
            DiscrepancyToleranceService toleranceService,
            DayStaffReadPort dayStaffReadPort,
            FuelPriceReadPort fuelPriceReadPort,
            JournalRepository journalRepository) {
        this.vendorShiftCloseRepository = vendorShiftCloseRepository;
        this.calculationEngine = calculationEngine;
        this.toleranceService = toleranceService;
        this.dayStaffReadPort = dayStaffReadPort;
        this.fuelPriceReadPort = fuelPriceReadPort;
        this.journalRepository = journalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DaySummary> findDaySummary(LocalDate journalDate) {
        if (journalDate == null) {
            return Optional.empty();
        }
        List<VendorShiftClose> closes = vendorShiftCloseRepository.findByJournalDate(journalDate);
        if (closes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(calculationEngine.aggregateDaySummary(journalDate, closes));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorCloseSummary> findVendorClosesByDate(LocalDate journalDate) {
        if (journalDate == null) {
            return List.of();
        }
        return vendorShiftCloseRepository.findByJournalDate(journalDate)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VendorCloseSummary> findVendorClose(LocalDate journalDate, String vendorDocument) {
        if (journalDate == null || vendorDocument == null) {
            return Optional.empty();
        }
        return vendorShiftCloseRepository.findByJournalDateAndVendorDocument(journalDate, vendorDocument)
                .map(this::toSummary);
    }

    /**
     * Calculates, validates, and persists individual vendor shift closes for a journal date.
     */
    @Transactional
    public List<VendorShiftClose> registerVendorCloses(LocalDate journalDate, List<VendorShiftClose> incomingCloses) {
        if (journalRepository != null && journalDate != null) {
            Journal journal = journalRepository.findByDate(journalDate).orElse(null);
            if (journal != null && isClosed(journal)) {
                throw new IllegalStateException(
                        "La jornada del " + journalDate + " ya está cerrada y no permite modificaciones.");
            }
        }
        BigDecimal effectiveTolerance = toleranceService.getEffectiveTolerance(journalDate);
        List<VendorShiftClose> evaluated = incomingCloses.stream()
                .map(c -> calculationEngine.calculateVendorClose(
                        c.id(),
                        journalDate,
                        c.vendorDocument(),
                        c.vendorName(),
                        c.shiftName(),
                        c.pumpNumber(),
                        c.cash(),
                        c.ga(),
                        c.credibanco(),
                        c.qr(),
                        c.redemptions(),
                        c.gallonsMotor(),
                        c.gallonsDiesel(),
                        c.gallonsExtra(),
                        c.gallonsMaxpro(),
                        c.basketDominusAmount(),
                        c.dominusTirilla(),
                        effectiveTolerance,
                        c.products()
                ))
                .toList();

        return vendorShiftCloseRepository.saveAll(evaluated);
    }

    public List<DayStaffReadPort.AssignedVendor> getAssignedStaffForDate(LocalDate date) {
        return dayStaffReadPort.findStaffForDate(date);
    }

    public Optional<BigDecimal> getEffectiveFuelPrice(String fuelType, LocalDate date) {
        return fuelPriceReadPort.findEffectivePrice(fuelType, date);
    }

    private boolean isClosed(Journal journal) {
        if (journal.getStatus() == JournalStatus.CLOSED) {
            return true;
        }
        return journal.getSteps() != null && journal.getSteps().stream()
                .anyMatch(s -> s.getStepNumber() != null && s.getStepNumber() == 6 && s.getStatus() == StepStatus.COMPLETED);
    }

    private VendorCloseSummary toSummary(VendorShiftClose close) {
        List<ProductSaleSummary> products = close.products() != null
                ? close.products().stream().map(p -> new ProductSaleSummary(
                p.sku(),
                p.productName(),
                p.category(),
                p.unitsSold(),
                p.unitPrice(),
                p.totalAmount()
        )).toList()
                : List.of();

        return new VendorCloseSummary(
                close.journalDate(),
                close.vendorDocument(),
                close.vendorName(),
                close.shiftName(),
                close.pumpNumber(),
                close.cash(),
                close.ga(),
                close.credibanco(),
                close.qr(),
                close.redemptions(),
                close.totalDelivered(),
                close.gallonsMotor(),
                close.gallonsDiesel(),
                close.gallonsExtra(),
                close.gallonsMaxpro(),
                close.basketDominusAmount(),
                close.dominusTirilla(),
                close.realDiscrepancy(),
                close.resultStatus() != null ? close.resultStatus().name() : DiscrepancyStatus.CUADRA.name(),
                products
        );
    }
}
