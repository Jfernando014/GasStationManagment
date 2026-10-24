package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Pure domain calculation engine for individual vendor shift closes (HU-24)
 * and day-level sales totals aggregation (S2-10).
 */
@Service
public class VendorCloseCalculationEngine {

    private static final int MONEY_SCALE = 2;
    private static final int VOLUME_SCALE = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Computes individual vendor shift close totals, real discrepancy and status.
     *
     * <p>Formulas:
     * total_delivered = cash + ga + credibanco + qr + redemptions
     * real_discrepancy = total_delivered - dominus_tirilla
     * status = CUADRA if |real_discrepancy| <= tolerance
     *          SOBRA  if real_discrepancy > tolerance
     *          FALTA  if real_discrepancy < -tolerance
     */
    public VendorShiftClose calculateVendorClose(
            UUID id,
            LocalDate journalDate,
            String vendorDocument,
            String vendorName,
            String shiftName,
            Integer pumpNumber,
            BigDecimal cash,
            BigDecimal ga,
            BigDecimal credibanco,
            BigDecimal qr,
            BigDecimal redemptions,
            BigDecimal gallonsMotor,
            BigDecimal gallonsDiesel,
            BigDecimal gallonsExtra,
            BigDecimal gallonsMaxpro,
            BigDecimal basketDominusAmount,
            BigDecimal dominusTirilla,
            BigDecimal effectiveTolerance,
            List<VendorCloseProduct> products
    ) {
        BigDecimal safeCash = safeMoney(cash);
        BigDecimal safeGa = safeMoney(ga);
        BigDecimal safeCredibanco = safeMoney(credibanco);
        BigDecimal safeQr = safeMoney(qr);
        BigDecimal safeRedemptions = safeMoney(redemptions);

        BigDecimal totalDelivered = safeCash
                .add(safeGa)
                .add(safeCredibanco)
                .add(safeQr)
                .add(safeRedemptions)
                .setScale(MONEY_SCALE, ROUNDING);

        BigDecimal safeTirilla = safeMoney(dominusTirilla);
        BigDecimal realDiscrepancy = totalDelivered.subtract(safeTirilla).setScale(MONEY_SCALE, ROUNDING);

        BigDecimal appliedTolerance = (effectiveTolerance != null && effectiveTolerance.compareTo(BigDecimal.ZERO) >= 0)
                ? effectiveTolerance.setScale(MONEY_SCALE, ROUNDING)
                : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);

        DiscrepancyStatus status = determineStatus(realDiscrepancy, appliedTolerance);

        return new VendorShiftClose(
                id,
                journalDate,
                vendorDocument,
                vendorName,
                shiftName,
                pumpNumber,
                safeCash,
                safeGa,
                safeCredibanco,
                safeQr,
                safeRedemptions,
                totalDelivered,
                safeVolume(gallonsMotor),
                safeVolume(gallonsDiesel),
                safeVolume(gallonsExtra),
                safeVolume(gallonsMaxpro),
                safeMoney(basketDominusAmount),
                safeTirilla,
                realDiscrepancy,
                appliedTolerance,
                status,
                products != null ? products : List.of()
        );
    }

    /**
     * Evaluates whether a discrepancy matches, exceeds or lacks against the tolerance threshold.
     */
    public DiscrepancyStatus determineStatus(BigDecimal discrepancy, BigDecimal tolerance) {
        BigDecimal safeDiscrepancy = safeMoney(discrepancy);
        BigDecimal safeTolerance = (tolerance != null && tolerance.compareTo(BigDecimal.ZERO) >= 0)
                ? tolerance.setScale(MONEY_SCALE, ROUNDING)
                : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);

        if (safeDiscrepancy.abs().compareTo(safeTolerance) <= 0) {
            return DiscrepancyStatus.CUADRA;
        } else if (safeDiscrepancy.compareTo(safeTolerance) > 0) {
            return DiscrepancyStatus.SOBRA;
        } else {
            return DiscrepancyStatus.FALTA;
        }
    }

    /**
     * Aggregates a list of vendor shift closes for a journal date into a consolidated DaySummary (S2-10).
     */
    public DaySummary aggregateDaySummary(LocalDate journalDate, List<VendorShiftClose> closes) {
        if (closes == null || closes.isEmpty()) {
            return new DaySummary(
                    journalDate,
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(VOLUME_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(VOLUME_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(VOLUME_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(VOLUME_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(VOLUME_SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING),
                    0,
                    List.of()
            );
        }

        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalGa = BigDecimal.ZERO;
        BigDecimal totalCredibanco = BigDecimal.ZERO;
        BigDecimal totalQr = BigDecimal.ZERO;
        BigDecimal totalRedemptions = BigDecimal.ZERO;
        BigDecimal totalDelivered = BigDecimal.ZERO;
        BigDecimal totalDominusTirilla = BigDecimal.ZERO;

        BigDecimal gallonsMotor = BigDecimal.ZERO;
        BigDecimal gallonsDiesel = BigDecimal.ZERO;
        BigDecimal gallonsExtra = BigDecimal.ZERO;
        BigDecimal gallonsMaxpro = BigDecimal.ZERO;
        BigDecimal basketDominusAmount = BigDecimal.ZERO;

        Map<String, ProductAccumulator> productMap = new LinkedHashMap<>();

        for (VendorShiftClose close : closes) {
            totalCash = totalCash.add(safeMoney(close.cash()));
            totalGa = totalGa.add(safeMoney(close.ga()));
            totalCredibanco = totalCredibanco.add(safeMoney(close.credibanco()));
            totalQr = totalQr.add(safeMoney(close.qr()));
            totalRedemptions = totalRedemptions.add(safeMoney(close.redemptions()));
            totalDelivered = totalDelivered.add(safeMoney(close.totalDelivered()));
            totalDominusTirilla = totalDominusTirilla.add(safeMoney(close.dominusTirilla()));

            gallonsMotor = gallonsMotor.add(safeVolume(close.gallonsMotor()));
            gallonsDiesel = gallonsDiesel.add(safeVolume(close.gallonsDiesel()));
            gallonsExtra = gallonsExtra.add(safeVolume(close.gallonsExtra()));
            gallonsMaxpro = gallonsMaxpro.add(safeVolume(close.gallonsMaxpro()));
            basketDominusAmount = basketDominusAmount.add(safeMoney(close.basketDominusAmount()));

            if (close.products() != null) {
                for (VendorCloseProduct p : close.products()) {
                    productMap.computeIfAbsent(p.sku(), k -> new ProductAccumulator(p.sku(), p.productName(), p.category()))
                            .add(p.unitsSold(), p.totalAmount());
                }
            }
        }

        BigDecimal totalDiscrepancy = totalDelivered.subtract(totalDominusTirilla).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal totalFuelGallons = gallonsMotor.add(gallonsDiesel).add(gallonsExtra).add(gallonsMaxpro)
                .setScale(VOLUME_SCALE, ROUNDING);

        int totalUnits = 0;
        List<ProductSaleSummary> consolidatedProducts = new ArrayList<>();
        for (ProductAccumulator acc : productMap.values()) {
            totalUnits += acc.unitsSold;
            consolidatedProducts.add(acc.toSummary());
        }

        return new DaySummary(
                journalDate,
                totalCash.setScale(MONEY_SCALE, ROUNDING),
                totalGa.setScale(MONEY_SCALE, ROUNDING),
                totalCredibanco.setScale(MONEY_SCALE, ROUNDING),
                totalQr.setScale(MONEY_SCALE, ROUNDING),
                totalRedemptions.setScale(MONEY_SCALE, ROUNDING),
                totalDelivered.setScale(MONEY_SCALE, ROUNDING),
                totalDominusTirilla.setScale(MONEY_SCALE, ROUNDING),
                totalDiscrepancy,
                gallonsMotor.setScale(VOLUME_SCALE, ROUNDING),
                gallonsDiesel.setScale(VOLUME_SCALE, ROUNDING),
                gallonsExtra.setScale(VOLUME_SCALE, ROUNDING),
                gallonsMaxpro.setScale(VOLUME_SCALE, ROUNDING),
                totalFuelGallons,
                basketDominusAmount.setScale(MONEY_SCALE, ROUNDING),
                totalUnits,
                consolidatedProducts
        );
    }

    private BigDecimal safeMoney(BigDecimal amount) {
        return amount != null ? amount.setScale(MONEY_SCALE, ROUNDING) : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
    }

    private BigDecimal safeVolume(BigDecimal volume) {
        return volume != null ? volume.setScale(VOLUME_SCALE, ROUNDING) : BigDecimal.ZERO.setScale(VOLUME_SCALE, ROUNDING);
    }

    private static class ProductAccumulator {
        private final String sku;
        private final String productName;
        private final String category;
        private int unitsSold = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        public ProductAccumulator(String sku, String productName, String category) {
            this.sku = sku;
            this.productName = productName;
            this.category = category;
        }

        public void add(int units, BigDecimal amount) {
            this.unitsSold += units;
            if (amount != null) {
                this.totalAmount = this.totalAmount.add(amount);
            }
        }

        public ProductSaleSummary toSummary() {
            BigDecimal unitPrice = unitsSold > 0
                    ? totalAmount.divide(BigDecimal.valueOf(unitsSold), MONEY_SCALE, ROUNDING)
                    : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
            return new ProductSaleSummary(
                    sku,
                    productName,
                    category,
                    unitsSold,
                    unitPrice,
                    totalAmount.setScale(MONEY_SCALE, ROUNDING)
            );
        }
    }
}
