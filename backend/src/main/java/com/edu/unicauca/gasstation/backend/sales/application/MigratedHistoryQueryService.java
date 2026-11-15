package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.MigratedHistoryQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.models.AugustMigratedHistoryCatalog;
import com.edu.unicauca.gasstation.backend.sales.domain.models.AugustMigratedHistoryCatalog.MigratedDaySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryCashBalanceSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryElectronicSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryVendorSaleItem;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes the migrated August 2026 snapshots through the public {@link MigratedHistoryQuery}.
 *
 * @author javiersolanop777
 */
@Service
@Transactional(readOnly = true)
public class MigratedHistoryQueryService implements MigratedHistoryQuery {

    @Override
    public Optional<MigratedDay> findDay(LocalDate date) {
        if (date == null) {
            return Optional.empty();
        }
        return AugustMigratedHistoryCatalog.findSnapshot(date).map(snapshot -> toDay(date, snapshot));
    }

    private MigratedDay toDay(LocalDate date, MigratedDaySnapshot snapshot) {
        return new MigratedDay(
                date,
                snapshot.vendorSales().stream().map(this::toVendorSale).toList(),
                toElectronic(snapshot.electronicSummary()),
                snapshot.variations().stream().map(this::toVariation).toList(),
                toCash(snapshot.cashBalance())
        );
    }

    private VendorSale toVendorSale(HistoryVendorSaleItem item) {
        return new VendorSale(
                item.vendorDocument(),
                item.vendorName(),
                item.shiftName(),
                item.totalGallons(),
                item.basketAmount(),
                item.totalSales(),
                item.totalDelivered(),
                item.discrepancy(),
                item.resultStatus()
        );
    }

    private ElectronicSummary toElectronic(HistoryElectronicSummary summary) {
        if (summary == null) {
            return null;
        }
        return new ElectronicSummary(
                summary.mastercard(),
                summary.visa(),
                summary.amex(),
                summary.qr(),
                summary.points(),
                summary.totalEds()
        );
    }

    private InventoryVariation toVariation(FuelVariationSummary variation) {
        return new InventoryVariation(
                variation.fuelType(),
                variation.initialGallons(),
                variation.entriesGallons(),
                variation.soldGallons(),
                variation.theoreticalGallons(),
                variation.measuredGallons(),
                variation.varianceGallons(),
                variation.toleranceGallons(),
                variation.outOfTolerance(),
                variation.status()
        );
    }

    private CashSummary toCash(HistoryCashBalanceSummary cash) {
        if (cash == null) {
            return null;
        }
        return new CashSummary(
                cash.planilla(),
                cash.previousCash(),
                cash.accumulatedSavings(),
                cash.dominusBalance()
        );
    }
}
