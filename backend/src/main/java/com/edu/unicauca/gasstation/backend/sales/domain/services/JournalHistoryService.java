package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.models.AugustMigratedHistoryCatalog;
import com.edu.unicauca.gasstation.backend.sales.domain.models.AugustMigratedHistoryCatalog.MigratedDaySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryCashBalanceSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryElectronicSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryVendorSaleItem;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalCalendarDay;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryCalendar;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryDayDetail;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Domain service for the monthly journal history calendar and migrated day detail (C-10, S3-06).
 */
@Service
@RequiredArgsConstructor
public class JournalHistoryService {

    private final JournalRepository journalRepository;
    private final SellerCloseQuery sellerCloseQuery;
    private final InventoryClosingReadPort inventoryClosingReadPort;

    @Transactional(readOnly = true)
    public JournalHistoryCalendar getMonthCalendar(YearMonth yearMonth) {
        YearMonth targetMonth = yearMonth != null ? yearMonth : YearMonth.of(2026, 8);
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();

        Map<LocalDate, Journal> journalsByDate = journalRepository.findByDateBetweenOrderByDateAsc(start, end)
                .stream()
                .collect(Collectors.toMap(Journal::getDate, j -> j, (a, b) -> a));

        List<JournalCalendarDay> days = new ArrayList<>();
        int closedInSystemCount = 0;
        int migratedDaysCount = 0;
        int inProgressCount = 0;
        int daysWithIssuesCount = 0;

        for (int dayNum = 1; dayNum <= targetMonth.lengthOfMonth(); dayNum++) {
            LocalDate date = targetMonth.atDay(dayNum);
            Journal journal = journalsByDate.get(date);
            JournalCalendarDay dayEntry = buildCalendarDay(date, dayNum, journal);
            days.add(dayEntry);

            if ("MIGRATED".equals(dayEntry.type())) {
                migratedDaysCount++;
            } else if ("JOURNAL".equals(dayEntry.type())) {
                if ("CLOSED".equals(dayEntry.status())) {
                    closedInSystemCount++;
                } else {
                    inProgressCount++;
                }
            }
            if (!dayEntry.markers().isEmpty()) {
                daysWithIssuesCount++;
            }
        }

        return new JournalHistoryCalendar(
                targetMonth.toString(),
                closedInSystemCount,
                migratedDaysCount,
                inProgressCount,
                daysWithIssuesCount,
                days
        );
    }

    @Transactional(readOnly = true)
    public JournalHistoryDayDetail getDayDetail(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        Optional<Journal> journalOpt = journalRepository.findByDate(date);
        List<VendorCloseSummary> dbVendorCloses = sellerCloseQuery.findVendorClosesByDate(date);
        Optional<DaySummary> dbDaySummary = sellerCloseQuery.findDaySummary(date);
        ClosingInventorySnapshot invSnapshot = inventoryClosingReadPort.findClosingSnapshot(date);

        if (journalOpt.isPresent()) {
            Journal journal = journalOpt.get();
            List<HistoryVendorSaleItem> vendorSales = mapVendorCloses(dbVendorCloses);
            boolean hasDiscrepancy = hasVendorDiscrepancy(dbVendorCloses)
                    || invSnapshot.variations().stream().anyMatch(FuelVariationSummary::outOfTolerance);
            boolean hasAlert = !invSnapshot.newAlerts().isEmpty();
            List<String> markers = buildMarkers(hasDiscrepancy, hasAlert);

            LocalDateTime closedAt = journal.getSteps() != null
                    ? journal.getSteps().stream()
                    .filter(s -> s.getStepNumber() != null && s.getStepNumber() == 6)
                    .map(JournalStep::getCompletedAt)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null)
                    : null;

            return new JournalHistoryDayDetail(
                    date,
                    "JOURNAL",
                    journal.getStatus().name(),
                    journal.getId(),
                    journal.getConsecutive(),
                    closedAt,
                    hasDiscrepancy,
                    hasAlert,
                    markers,
                    vendorSales,
                    mapElectronicSummary(dbDaySummary.orElse(null)),
                    invSnapshot.variations(),
                    null,
                    invSnapshot.newAlerts()
            );
        }

        if (!dbVendorCloses.isEmpty()) {
            List<HistoryVendorSaleItem> vendorSales = mapVendorCloses(dbVendorCloses);
            boolean hasDiscrepancy = hasVendorDiscrepancy(dbVendorCloses)
                    || invSnapshot.variations().stream().anyMatch(FuelVariationSummary::outOfTolerance);
            boolean hasAlert = !invSnapshot.newAlerts().isEmpty();
            return new JournalHistoryDayDetail(
                    date,
                    "MIGRATED",
                    "CLOSED",
                    null,
                    null,
                    null,
                    hasDiscrepancy,
                    hasAlert,
                    buildMarkers(hasDiscrepancy, hasAlert),
                    vendorSales,
                    mapElectronicSummary(dbDaySummary.orElse(null)),
                    invSnapshot.variations(),
                    null,
                    invSnapshot.newAlerts()
            );
        }

        Optional<MigratedDaySnapshot> seedOpt = AugustMigratedHistoryCatalog.findSnapshot(date);
        if (seedOpt.isPresent()) {
            MigratedDaySnapshot seed = seedOpt.get();
            boolean hasAlert = !invSnapshot.newAlerts().isEmpty();
            List<FuelVariationSummary> variations = !invSnapshot.variations().isEmpty()
                    ? invSnapshot.variations()
                    : seed.variations();
            return new JournalHistoryDayDetail(
                    date,
                    "MIGRATED",
                    "CLOSED",
                    null,
                    null,
                    null,
                    seed.hasDiscrepancy(),
                    hasAlert,
                    buildMarkers(seed.hasDiscrepancy(), hasAlert),
                    seed.vendorSales(),
                    seed.electronicSummary(),
                    variations,
                    seed.cashBalance(),
                    invSnapshot.newAlerts()
            );
        }

        return new JournalHistoryDayDetail(
                date,
                "EMPTY",
                "PENDING",
                null,
                null,
                null,
                false,
                false,
                List.of(),
                List.of(),
                null,
                List.of(),
                null,
                List.of()
        );
    }

    private JournalCalendarDay buildCalendarDay(LocalDate date, int dayOfMonth, Journal journal) {
        List<VendorCloseSummary> vendorCloses = sellerCloseQuery.findVendorClosesByDate(date);
        ClosingInventorySnapshot invSnapshot = inventoryClosingReadPort.findClosingSnapshot(date);
        boolean hasAlert = !invSnapshot.newAlerts().isEmpty();

        if (journal != null) {
            boolean hasDiscrepancy = hasVendorDiscrepancy(vendorCloses)
                    || invSnapshot.variations().stream().anyMatch(FuelVariationSummary::outOfTolerance);
            return new JournalCalendarDay(
                    date,
                    dayOfMonth,
                    "JOURNAL",
                    journal.getStatus() == JournalStatus.CLOSED ? "CLOSED" : "OPEN",
                    journal.getId(),
                    journal.getConsecutive(),
                    hasDiscrepancy,
                    hasAlert,
                    buildMarkers(hasDiscrepancy, hasAlert)
            );
        }

        if (!vendorCloses.isEmpty()) {
            boolean hasDiscrepancy = hasVendorDiscrepancy(vendorCloses);
            return new JournalCalendarDay(
                    date,
                    dayOfMonth,
                    "MIGRATED",
                    "CLOSED",
                    null,
                    null,
                    hasDiscrepancy,
                    hasAlert,
                    buildMarkers(hasDiscrepancy, hasAlert)
            );
        }

        if (AugustMigratedHistoryCatalog.isMigratedSeedDate(date)) {
            boolean hasDiscrepancy = AugustMigratedHistoryCatalog.hasSeedDiscrepancy(date);
            return new JournalCalendarDay(
                    date,
                    dayOfMonth,
                    "MIGRATED",
                    "CLOSED",
                    null,
                    null,
                    hasDiscrepancy,
                    hasAlert,
                    buildMarkers(hasDiscrepancy, hasAlert)
            );
        }

        return new JournalCalendarDay(
                date,
                dayOfMonth,
                "EMPTY",
                "PENDING",
                null,
                null,
                false,
                false,
                List.of()
        );
    }

    private boolean hasVendorDiscrepancy(List<VendorCloseSummary> closes) {
        return closes != null && closes.stream()
                .anyMatch(c -> c.resultStatus() != null && !"CUADRA".equalsIgnoreCase(c.resultStatus())
                        && !"OK".equalsIgnoreCase(c.resultStatus()));
    }

    private List<String> buildMarkers(boolean hasDiscrepancy, boolean hasAlert) {
        List<String> markers = new ArrayList<>(2);
        if (hasDiscrepancy) {
            markers.add("descuadre");
        }
        if (hasAlert) {
            markers.add("alerta");
        }
        return List.copyOf(markers);
    }

    private List<HistoryVendorSaleItem> mapVendorCloses(List<VendorCloseSummary> closes) {
        if (closes == null) {
            return List.of();
        }
        return closes.stream()
                .map(c -> {
                    BigDecimal gallons = safe(c.gallonsMotor())
                            .add(safe(c.gallonsDiesel()))
                            .add(safe(c.gallonsExtra()))
                            .add(safe(c.gallonsMaxpro()));
                    return new HistoryVendorSaleItem(
                            c.vendorDocument(),
                            c.vendorName(),
                            c.shiftName(),
                            gallons,
                            safe(c.basketDominusAmount()),
                            safe(c.dominusTirilla()),
                            safe(c.totalDelivered()),
                            safe(c.realDiscrepancy()),
                            c.resultStatus() != null ? c.resultStatus() : "CUADRA"
                    );
                })
                .toList();
    }

    private HistoryElectronicSummary mapElectronicSummary(DaySummary daySummary) {
        if (daySummary == null) {
            return null;
        }
        BigDecimal credibanco = safe(daySummary.totalCredibanco());
        return new HistoryElectronicSummary(
                credibanco,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                safe(daySummary.totalQr()),
                safe(daySummary.totalRedemptions()),
                credibanco
        );
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
