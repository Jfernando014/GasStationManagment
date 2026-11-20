package com.edu.unicauca.gasstation.backend.incentives;

import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.DIESEL_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.DIESEL_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.DIESEL_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.SUPER_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.SUPER_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.SUPER_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.additive;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.rule;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.sale;

import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemoryCatalog;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Units and incentives of the sheet "INCENTIVOS PETROLABS BELLAVISTA 2026.xlsx" (Hoja 1), November 2025 to April 2026.
 *
 * <p>The units of each seller and the pesos of the sheet are copied as they appear: {@code monthTotal} is the TOTAL
 * column of the month and {@code accumulated} is what the sheet adds up from November 2025. The sheet shows the
 * accumulated column from February on; the earlier ones are the running sum of the monthly totals. The expected
 * values are written down, not worked out here, so the tests compare the service with the sheet and not with itself.
 *
 * <p>Values per unit of the sheet: gasoline additive $800 (Nov to Jan), $1,000 (Feb), $1,200 (from March); octane
 * elevator $2,000 always; super concentrate $500 (Nov to Jan), $800 (Feb), $1,000 (from March); diesel additive
 * $1,500 always. A blank cell of the sheet is 0 here, and a seller with 0 units has no sale row.
 */
public final class BellavistaIncentiveSheet {

    public static final String ANA = "1001";
    public static final String CARLOS = "1002";
    public static final String WILTOM = "1003";
    public static final String MONICA = "1004";

    /** One seller in one month: units per additive and what the sheet says they earned. */
    public record Entry(YearMonth month, String document, String name, int gasoline, int octane,
                        int superConcentrate, int diesel, long monthTotal, long accumulated) {
    }

    private static final String ANA_NAME = "ANA MILENA VILLAMIL";
    private static final String CARLOS_NAME = "CARLOS ANDRÉS PAREDES";
    private static final String WILTOM_NAME = "WILTOM ANDRÉS PAZ";
    private static final String MONICA_NAME = "MÓNICA SIRLEY DORADO";

    private static final YearMonth NOV = YearMonth.of(2025, 11);
    private static final YearMonth DEC = YearMonth.of(2025, 12);
    private static final YearMonth JAN = YearMonth.of(2026, 1);
    private static final YearMonth FEB = YearMonth.of(2026, 2);
    private static final YearMonth MAR = YearMonth.of(2026, 3);
    private static final YearMonth APR = YearMonth.of(2026, 4);

    private static final List<Entry> ENTRIES = List.of(
            new Entry(NOV, ANA, ANA_NAME, 6, 7, 0, 0, 18_800, 18_800),
            new Entry(NOV, CARLOS, CARLOS_NAME, 16, 0, 0, 0, 12_800, 12_800),
            new Entry(NOV, WILTOM, WILTOM_NAME, 26, 4, 0, 0, 28_800, 28_800),

            new Entry(DEC, ANA, ANA_NAME, 8, 8, 17, 0, 30_900, 49_700),
            new Entry(DEC, CARLOS, CARLOS_NAME, 18, 1, 3, 0, 17_900, 30_700),
            new Entry(DEC, WILTOM, WILTOM_NAME, 39, 9, 4, 0, 51_200, 80_000),

            new Entry(JAN, ANA, ANA_NAME, 54, 23, 17, 3, 102_200, 151_900),
            new Entry(JAN, CARLOS, CARLOS_NAME, 9, 6, 11, 0, 24_700, 55_400),
            new Entry(JAN, WILTOM, WILTOM_NAME, 13, 3, 4, 0, 18_400, 98_400),

            new Entry(FEB, ANA, ANA_NAME, 34, 15, 16, 1, 78_300, 230_200),
            new Entry(FEB, CARLOS, CARLOS_NAME, 6, 0, 4, 1, 10_700, 66_100),
            new Entry(FEB, WILTOM, WILTOM_NAME, 16, 1, 7, 0, 23_600, 122_000),

            new Entry(MAR, ANA, ANA_NAME, 178, 14, 47, 0, 288_600, 518_800),
            new Entry(MAR, CARLOS, CARLOS_NAME, 47, 0, 8, 0, 64_400, 130_500),
            new Entry(MAR, WILTOM, WILTOM_NAME, 65, 3, 54, 0, 138_000, 260_000),

            new Entry(APR, ANA, ANA_NAME, 174, 9, 29, 1, 257_300, 776_100),
            new Entry(APR, CARLOS, CARLOS_NAME, 61, 1, 2, 0, 77_200, 207_700),
            new Entry(APR, WILTOM, WILTOM_NAME, 39, 0, 28, 0, 74_800, 334_800),
            new Entry(APR, MONICA, MONICA_NAME, 31, 3, 9, 0, 52_200, 52_200));

    private BellavistaIncentiveSheet() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** The "TOTAL" cell of each month of the sheet: what all the sellers earned together. */
    public static long sheetTotal(YearMonth month) {
        return switch (month.toString()) {
            case "2025-11" -> 60_400;
            case "2025-12" -> 100_000;
            case "2026-01" -> 145_300;
            case "2026-02" -> 112_600;
            case "2026-03" -> 491_000;
            case "2026-04" -> 461_500;
            default -> throw new IllegalArgumentException("The sheet has no total for " + month);
        };
    }

    /** The values per unit of the sheet as incentive rules, with the validity of each change. */
    public static List<IncentiveRule> rules() {
        return List.of(
                rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 1)),
                rule(GASOLINE_ID, 1_200, LocalDate.of(2026, 3, 1)),
                rule(OCTANE_ID, 2_000, LocalDate.of(2025, 11, 1)),
                rule(SUPER_ID, 500, LocalDate.of(2025, 11, 1)),
                rule(SUPER_ID, 800, LocalDate.of(2026, 2, 1)),
                rule(SUPER_ID, 1_000, LocalDate.of(2026, 3, 1)),
                rule(DIESEL_ID, 1_500, LocalDate.of(2025, 11, 1)));
    }

    /** The four additives with the sale price printed in the heading of the sheet. */
    public static InMemoryCatalog catalog() {
        return new InMemoryCatalog()
                .with(GASOLINE_SKU, additive(GASOLINE_ID, GASOLINE_NAME, true, 4_500))
                .with(OCTANE_SKU, additive(OCTANE_ID, OCTANE_NAME, true, 13_500))
                .with(SUPER_SKU, additive(SUPER_ID, SUPER_NAME, true, 6_300))
                .with(DIESEL_SKU, additive(DIESEL_ID, DIESEL_NAME, true, 11_000));
    }

    /**
     * The units of the sheet as daily sales. The units of each additive in a month are split in two days (the 5th and
     * the 20th), so adding them up is part of what the tests check.
     */
    public static List<AdditiveSaleUnits> sales() {
        List<AdditiveSaleUnits> sales = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            addUnits(sales, entry, GASOLINE_SKU, GASOLINE_NAME, entry.gasoline());
            addUnits(sales, entry, OCTANE_SKU, OCTANE_NAME, entry.octane());
            addUnits(sales, entry, SUPER_SKU, SUPER_NAME, entry.superConcentrate());
            addUnits(sales, entry, DIESEL_SKU, DIESEL_NAME, entry.diesel());
        }
        return sales;
    }

    private static void addUnits(List<AdditiveSaleUnits> sales, Entry entry, String sku, String productName,
                                 int units) {
        if (units == 0) {
            return;
        }
        int first = units / 2;
        if (first > 0) {
            sales.add(sale(entry.month().atDay(5), entry.document(), entry.name(), sku, productName, first));
        }
        sales.add(sale(entry.month().atDay(20), entry.document(), entry.name(), sku, productName, units - first));
    }
}
