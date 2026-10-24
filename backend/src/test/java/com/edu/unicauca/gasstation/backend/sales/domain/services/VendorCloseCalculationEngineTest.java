package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorCloseCalculationEngineTest {

    private VendorCloseCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new VendorCloseCalculationEngine();
    }

    @Test
    @DisplayName("August 5, 2026 - Mónica (12-7) computes exact total delivered and discrepancy of +$13.13 (Sobra)")
    void monicaClose_computesExactAugust5Data_withSobra13_13() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        BigDecimal cash = new BigDecimal("2373000.00");
        BigDecimal ga = BigDecimal.ZERO;
        BigDecimal credibanco = new BigDecimal("1166042.00");
        BigDecimal qr = new BigDecimal("351000.00");
        BigDecimal redemptions = BigDecimal.ZERO;
        BigDecimal dominusTirilla = new BigDecimal("3890028.87");
        BigDecimal zeroTolerance = BigDecimal.ZERO;

        VendorShiftClose close = engine.calculateVendorClose(
                UUID.randomUUID(), date, "1001", "Mónica D.", "12-7", 1,
                cash, ga, credibanco, qr, redemptions,
                new BigDecimal("150.500"), new BigDecimal("80.200"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("45000.00"), dominusTirilla, zeroTolerance, List.of()
        );

        assertThat(close.totalDelivered()).isEqualByComparingTo("3890042.00");
        assertThat(close.realDiscrepancy()).isEqualByComparingTo("13.13");
        assertThat(close.resultStatus()).isEqualTo(DiscrepancyStatus.SOBRA);
    }

    @Test
    @DisplayName("August 5, 2026 - Carlos (DIA6) computes exact total delivered and discrepancy of +$531.42 (Sobra)")
    void carlosClose_computesExactAugust5Data_withSobra531_42() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        BigDecimal cash = new BigDecimal("1847000.00");
        BigDecimal ga = BigDecimal.ZERO;
        BigDecimal credibanco = new BigDecimal("2296042.00");
        BigDecimal qr = new BigDecimal("143000.00");
        BigDecimal redemptions = BigDecimal.ZERO;
        BigDecimal dominusTirilla = new BigDecimal("4285510.58");
        BigDecimal zeroTolerance = BigDecimal.ZERO;

        VendorShiftClose close = engine.calculateVendorClose(
                UUID.randomUUID(), date, "1002", "Carlos P.", "DIA6", 1,
                cash, ga, credibanco, qr, redemptions,
                new BigDecimal("180.100"), new BigDecimal("95.000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("60000.00"), dominusTirilla, zeroTolerance, List.of()
        );

        assertThat(close.totalDelivered()).isEqualByComparingTo("4286042.00");
        assertThat(close.realDiscrepancy()).isEqualByComparingTo("531.42");
        assertThat(close.resultStatus()).isEqualTo(DiscrepancyStatus.SOBRA);
    }

    @Test
    @DisplayName("August 5, 2026 - Wiltom (NOCHE) computes exact total delivered and discrepancy of +$1338.05 (Sobra)")
    void wiltomClose_computesExactAugust5Data_withSobra1338_05() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        BigDecimal cash = new BigDecimal("3310000.00");
        BigDecimal ga = BigDecimal.ZERO;
        BigDecimal credibanco = new BigDecimal("2228616.00");
        BigDecimal qr = new BigDecimal("424000.00");
        BigDecimal redemptions = BigDecimal.ZERO;
        BigDecimal dominusTirilla = new BigDecimal("5961277.95");
        BigDecimal zeroTolerance = BigDecimal.ZERO;

        VendorShiftClose close = engine.calculateVendorClose(
                UUID.randomUUID(), date, "1003", "Wiltom C.", "NOCHE", 1,
                cash, ga, credibanco, qr, redemptions,
                new BigDecimal("240.000"), new BigDecimal("120.500"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("80000.00"), dominusTirilla, zeroTolerance, List.of()
        );

        assertThat(close.totalDelivered()).isEqualByComparingTo("5962616.00");
        assertThat(close.realDiscrepancy()).isEqualByComparingTo("1338.05");
        assertThat(close.resultStatus()).isEqualTo(DiscrepancyStatus.SOBRA);
    }

    @Test
    @DisplayName("August 5, 2026 - Consolidated DaySummary matches day cash $7.530.000 and total delivered $14.138.700")
    void aggregateDaySummary_computesExactAugust5Totals_includingCash7530000() {
        LocalDate date = LocalDate.of(2026, 8, 5);

        VendorShiftClose monica = engine.calculateVendorClose(
                UUID.randomUUID(), date, "1001", "Mónica D.", "12-7", 1,
                new BigDecimal("2373000.00"), BigDecimal.ZERO, new BigDecimal("1166042.00"), new BigDecimal("351000.00"), BigDecimal.ZERO,
                new BigDecimal("150.000"), new BigDecimal("50.000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("45000.00"), new BigDecimal("3890028.87"), BigDecimal.ZERO,
                List.of(new VendorCloseProduct(UUID.randomUUID(), "LUB-01", "Lubricante 20W50", "CANASTA", 2, new BigDecimal("22500.00"), new BigDecimal("45000.00")))
        );

        VendorShiftClose carlos = engine.calculateVendorClose(
                UUID.randomUUID(), date, "1002", "Carlos P.", "DIA6", 1,
                new BigDecimal("1847000.00"), BigDecimal.ZERO, new BigDecimal("2296042.00"), new BigDecimal("143000.00"), BigDecimal.ZERO,
                new BigDecimal("180.000"), new BigDecimal("60.000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("60000.00"), new BigDecimal("4285510.58"), BigDecimal.ZERO,
                List.of(new VendorCloseProduct(UUID.randomUUID(), "LUB-02", "Aditivo Gasolina", "CANASTA", 3, new BigDecimal("20000.00"), new BigDecimal("60000.00")))
        );

        VendorShiftClose wiltom = engine.calculateVendorClose(
                UUID.randomUUID(), date, "1003", "Wiltom C.", "NOCHE", 1,
                new BigDecimal("3310000.00"), BigDecimal.ZERO, new BigDecimal("2228616.00"), new BigDecimal("424000.00"), BigDecimal.ZERO,
                new BigDecimal("240.000"), new BigDecimal("80.000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("80000.00"), new BigDecimal("5961277.95"), BigDecimal.ZERO,
                List.of(new VendorCloseProduct(UUID.randomUUID(), "LUB-01", "Lubricante 20W50", "CANASTA", 1, new BigDecimal("22500.00"), new BigDecimal("22500.00")))
        );

        DaySummary daySummary = engine.aggregateDaySummary(date, List.of(monica, carlos, wiltom));

        // S2-10 core calibration: exact total cash for the day is $7.530.000
        assertThat(daySummary.totalCash()).isEqualByComparingTo("7530000.00");
        assertThat(daySummary.totalCredibanco()).isEqualByComparingTo("5690700.00");
        assertThat(daySummary.totalQr()).isEqualByComparingTo("918000.00");
        assertThat(daySummary.totalDelivered()).isEqualByComparingTo("14138700.00");
        assertThat(daySummary.totalDominusTirilla()).isEqualByComparingTo("14136817.40");
        assertThat(daySummary.totalDiscrepancy()).isEqualByComparingTo("1882.60");
        assertThat(daySummary.totalProductUnits()).isEqualTo(6);
        assertThat(daySummary.products()).hasSize(2);
    }

    @Test
    @DisplayName("Tolerance threshold policy: evaluates CUADRA when discrepancy is within tolerance limit")
    void toleranceEvaluation_properlyCategorizesCuadraSobraAndFalta() {
        BigDecimal tolerance = new BigDecimal("20.00");

        // +$13.13 within tolerance $20.00 -> CUADRA
        assertThat(engine.determineStatus(new BigDecimal("13.13"), tolerance)).isEqualTo(DiscrepancyStatus.CUADRA);

        // -$15.00 within tolerance $20.00 -> CUADRA
        assertThat(engine.determineStatus(new BigDecimal("-15.00"), tolerance)).isEqualTo(DiscrepancyStatus.CUADRA);

        // +$20.00 exact limit -> CUADRA
        assertThat(engine.determineStatus(new BigDecimal("20.00"), tolerance)).isEqualTo(DiscrepancyStatus.CUADRA);

        // +$20.01 above limit -> SOBRA
        assertThat(engine.determineStatus(new BigDecimal("20.01"), tolerance)).isEqualTo(DiscrepancyStatus.SOBRA);

        // -$20.01 below limit -> FALTA
        assertThat(engine.determineStatus(new BigDecimal("-20.01"), tolerance)).isEqualTo(DiscrepancyStatus.FALTA);
    }
}
