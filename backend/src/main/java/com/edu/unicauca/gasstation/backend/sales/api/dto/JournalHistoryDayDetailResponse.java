package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryCashBalanceSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryElectronicSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryVendorSaleItem;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryDayDetail;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Detalle de un día del historial de jornadas, con los nombres que usa el frontend")
public record JournalHistoryDayDetailResponse(
        LocalDate date,
        String source,
        String state,
        Long journalId,
        Long consecutive,
        LocalDateTime closedAt,
        List<String> markers,
        List<VendorSaleItem> vendorSales,
        CredibancoSummary credibancoSummary,
        List<FuelVariationItem> fuelVariations,
        CashBalanceSummary cashBalanceSummary
) {

    public record VendorSaleItem(
            String vendorName,
            String shift,
            BigDecimal gallons,
            BigDecimal basketsAmount,
            BigDecimal totalSalesAmount,
            BigDecimal discrepancyAmount
    ) {}

    public record CredibancoSummary(
            BigDecimal mastercardAmount,
            BigDecimal visaAmount,
            BigDecimal qrAmount,
            BigDecimal totalEdsAmount
    ) {}

    public record FuelVariationItem(
            String fuelName,
            BigDecimal soldGallons,
            BigDecimal theoreticalGallons,
            BigDecimal physicalGallons,
            BigDecimal variationGallons
    ) {}

    public record CashBalanceSummary(
            Long sheetNumber,
            BigDecimal previousCashAmount,
            BigDecimal accumulatedSavingsAmount,
            BigDecimal dominusBalanceAmount
    ) {}

    public static JournalHistoryDayDetailResponse from(JournalHistoryDayDetail detail) {
        return new JournalHistoryDayDetailResponse(
                detail.date(),
                detail.type(),
                JournalHistoryMonthResponse.state(detail.type(), detail.status()),
                detail.journalId(),
                detail.consecutive(),
                detail.closedAt(),
                JournalHistoryMonthResponse.markers(detail.markers()),
                detail.vendorSales().stream().map(JournalHistoryDayDetailResponse::toVendorSale).toList(),
                toCredibanco(detail.electronicSummary()),
                detail.variations().stream().map(JournalHistoryDayDetailResponse::toVariation).toList(),
                toCashBalance(detail.cashBalance())
        );
    }

    private static VendorSaleItem toVendorSale(HistoryVendorSaleItem sale) {
        return new VendorSaleItem(
                sale.vendorName(),
                sale.shiftName(),
                sale.totalGallons(),
                sale.basketAmount(),
                sale.totalSales(),
                sale.discrepancy()
        );
    }

    private static CredibancoSummary toCredibanco(HistoryElectronicSummary summary) {
        if (summary == null) {
            return null;
        }
        return new CredibancoSummary(summary.mastercard(), summary.visa(), summary.qr(), summary.totalEds());
    }

    private static FuelVariationItem toVariation(FuelVariationSummary variation) {
        return new FuelVariationItem(
                JournalClosingSummaryResponse.fuelName(variation.fuelType()),
                variation.soldGallons(),
                variation.theoreticalGallons(),
                variation.measuredGallons(),
                variation.varianceGallons()
        );
    }

    private static CashBalanceSummary toCashBalance(HistoryCashBalanceSummary summary) {
        if (summary == null) {
            return null;
        }
        return new CashBalanceSummary(
                summary.planilla(),
                summary.previousCash(),
                summary.accumulatedSavings(),
                summary.dominusBalance()
        );
    }
}
