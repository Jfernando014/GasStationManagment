package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Figures of 05/08/2026 taken from "Control Juan Clavijo Agosto 2026" (sheet 05) and
 * "5 REPORTE CREDIBANCO BELLAVISTA AGOSTO 2026". The split of Mastercard/Visa between vendors is SYNTHETIC: the
 * workbook only keeps the sum of the vouchers; each vendor's Credibanco, QR and redemptions are the real planilla
 * values. Shifts of the day: Ana DESCANSO, Carlos DIA6, Mónica 12-7, Wiltom NOCHE (flow document).
 */
final class ReconciliationTestData {

    static final LocalDate DAY = LocalDate.of(2026, 8, 5);
    static final UUID MONICA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final UUID CARLOS = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    static final UUID WILTOM = UUID.fromString("00000000-0000-0000-0000-00000000000d");
    static final Map<UUID, String> NAMES = Map.of(MONICA, "Mónica Dorado", ANA, "Ana Milena Villamil",
            CARLOS, "Carlos Andrés Paredes", WILTOM, "Wiltom Andrés Paz");

    private ReconciliationTestData() {
    }

    static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    /** Vendor with vouchers and the planilla figures; dawn amounts are zero. */
    static VendorDeclaration vendor(UUID id, String shift, String mastercard, String visa, String credibanco,
                                    String qr, String redemptions) {
        return new VendorDeclaration(id, shift, money(mastercard), money(visa), money("0"), null, null, null,
                money(credibanco), money(qr), money(redemptions));
    }

    /** Planillas of 05/08: Credibanco 1.354.436 + 0 + 2.019.760 + 2.317.116 = 5.691.312. */
    static java.util.List<VendorDeclaration> august5() {
        return java.util.List.of(
                vendor(MONICA, "12-7", "800000", "554436", "1354436", "162606", "0"),
                vendor(ANA, "DESCANSO", "0", "0", "0", "0", "0"),
                vendor(CARLOS, "DIA6", "1000000", "1019760", "2019760", "377902", "41380"),
                vendor(WILTOM, "NOCHE", "1818384", "498732", "2317116", "335500", "0"));
    }

    /** Days 1 to 4 of the workbook (Mastercard, Visa, QR). */
    static Map<LocalDate, DayFranchiseTotals> august1to4() {
        long[][] days = {
                {3094013, 1941683, 711315},
                {1951292, 946566, 506496},
                {3132866, 1409154, 931222},
                {2151689, 1178512, 565243}};
        Map<LocalDate, DayFranchiseTotals> map = new java.util.TreeMap<>();
        for (int i = 0; i < days.length; i++) {
            map.put(LocalDate.of(2026, 8, i + 1), new DayFranchiseTotals(money("0"),
                    money(String.valueOf(days[i][0])), money(String.valueOf(days[i][1])),
                    money("0"), money("0"), money("0"), money(String.valueOf(days[i][2]))));
        }
        return map;
    }

    /** "VALORES MES ANTERIOR" row of the workbook. */
    static PeriodTotals previousMonth() {
        return PeriodTotals.of(money("70829095"), money("34166441"), money("0"), money("21926590"), "BASELINE");
    }
}
