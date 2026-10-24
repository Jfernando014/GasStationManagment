package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation of the public SellerCloseQuery contract for cross-module consumption.
 */
@Service
@RequiredArgsConstructor
public class SellerCloseQueryServiceImpl implements SellerCloseQuery {

    private final VendorShiftCloseRepository vendorShiftCloseRepository;
    private final VendorCloseCalculationEngine calculationEngine;
    private final DiscrepancyToleranceService toleranceService;
    private final DayStaffReadPort dayStaffReadPort;
    private final FuelPriceReadPort fuelPriceReadPort;

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
