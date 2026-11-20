package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import static com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet.ANA;
import static com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet.CARLOS;
import static com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet.MONICA;
import static com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet.WILTOM;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.ACCUMULATED_FROM;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.DIESEL_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.DIESEL_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.SUPER_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.SUPER_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.additive;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.clockAt;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.rule;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.sale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;

import com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet;
import com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet.Entry;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemoryCatalog;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemoryRules;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemorySales;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AccumulationStart;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveLine;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.MonthlyIncentives;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.UnresolvedAdditive;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.UnresolvedReason;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.VendorIncentive;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Monthly calculation of the incentives. The first group of tests reproduces "INCENTIVOS PETROLABS BELLAVISTA
 * 2026.xlsx" (see {@link BellavistaIncentiveSheet}); the rest build small cases for what the sheet does not show,
 * such as a change of value inside a month or units without a rule.
 */
class IncentiveCalculationServiceTest {

    private static final String NOW = "2026-04-20T15:00:00Z";

    private InMemorySales sales;
    private InMemoryRules rules;
    private InMemoryCatalog catalog;

    @BeforeEach
    void setUp() {
        sales = new InMemorySales();
        rules = new InMemoryRules();
        catalog = BellavistaIncentiveSheet.catalog();
    }

    private IncentiveCalculationService service() {
        return serviceAt(NOW);
    }

    private IncentiveCalculationService serviceAt(String instant) {
        return new IncentiveCalculationService(sales, rules, catalog, new StandardUnitValueCalculator(),
                new AccumulationStart(ACCUMULATED_FROM), clockAt(instant));
    }

    private void loadTheSheet() {
        sales.with(BellavistaIncentiveSheet.sales());
        rules.with(BellavistaIncentiveSheet.rules());
    }

    private static VendorIncentive vendor(MonthlyIncentives result, String document) {
        return result.vendors().stream()
                .filter(vendor -> vendor.vendorDocument().equals(document))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No seller " + document + " in " + result.month()));
    }

    private static IncentiveLine line(VendorIncentive vendor, String sku) {
        return vendor.lines().stream()
                .filter(candidate -> candidate.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line of " + sku));
    }

    private static BigDecimal pesos(long amount) {
        return BigDecimal.valueOf(amount);
    }

    static Stream<Entry> sheetEntries() {
        return BellavistaIncentiveSheet.entries().stream();
    }

    // ---- the sheet of the client ----

    @ParameterizedTest
    @MethodSource("sheetEntries")
    void reproducesTheMonthAndTheAccumulatedOfEverySellerOfTheSheet(Entry entry) {
        loadTheSheet();

        VendorIncentive vendor = vendor(service().calculateMonth(entry.month()), entry.document());

        assertThat(vendor.monthTotal()).as("month total").isEqualByComparingTo(pesos(entry.monthTotal()));
        assertThat(vendor.accumulatedTotal()).as("accumulated").isEqualByComparingTo(pesos(entry.accumulated()));
        assertThat(vendor.vendorName()).isEqualTo(entry.name());
        assertThat(vendor.unresolved()).isEmpty();
        assertThat(vendor.accumulatedUnresolvedUnits()).isZero();
    }

    @ParameterizedTest
    @MethodSource("sheetMonths")
    void aMonthListsExactlyTheSellersWhoSoldAndAddsUpToTheTotalOfTheSheet(YearMonth month) {
        loadTheSheet();

        MonthlyIncentives result = service().calculateMonth(month);

        List<String> expected = BellavistaIncentiveSheet.entries().stream()
                .filter(entry -> entry.month().equals(month))
                .map(Entry::document)
                .toList();
        assertThat(result.vendors()).extracting(VendorIncentive::vendorDocument)
                .containsExactlyInAnyOrderElementsOf(expected);
        BigDecimal total = result.vendors().stream().map(VendorIncentive::monthTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(pesos(BellavistaIncentiveSheet.sheetTotal(month)));
        assertThat(result.month()).isEqualTo(month);
        assertThat(result.accumulatedFrom()).isEqualTo(ACCUMULATED_FROM);
    }

    static Stream<YearMonth> sheetMonths() {
        return BellavistaIncentiveSheet.entries().stream().map(Entry::month).distinct();
    }

    @Test
    void februaryPaysTheNewValuesWhileTheAccumulatedKeepsWhatEarlierMonthsPaidAtTheOldOnes() {
        loadTheSheet();

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        // Sheet, February: 34 x $1,000, 15 x $2,000, 16 x $800 and 1 x $1,500
        assertThat(ana.lines()).extracting(IncentiveLine::sku, IncentiveLine::units,
                        line -> line.valuePerUnit().intValue(), line -> line.subtotal().intValue())
                .containsExactlyInAnyOrder(
                        tuple(GASOLINE_SKU, 34, 1_000, 34_000),
                        tuple(OCTANE_SKU, 15, 2_000, 30_000),
                        tuple(SUPER_SKU, 16, 800, 12_800),
                        tuple(DIESEL_SKU, 1, 1_500, 1_500));
        assertThat(ana.monthTotal()).isEqualByComparingTo(pesos(78_300));
        // The same units in January were paid at $800 and $500: 18,800 + 30,900 + 102,200 before February
        assertThat(ana.accumulatedTotal()).isEqualByComparingTo(pesos(230_200));
    }

    @Test
    void theLinesAreSortedByProductNameAndEachOneCarriesItsMonthPeriod() {
        loadTheSheet();

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).extracting(IncentiveLine::productName)
                .containsExactly(DIESEL_NAME, GASOLINE_NAME, OCTANE_NAME, SUPER_NAME);
        // The February values start on the 1st and the next change (March 1st) is outside the month
        assertThat(ana.lines()).allSatisfy(line -> {
            assertThat(line.periodStart()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(line.periodEnd()).isNull();
        });
    }

    @Test
    void theAccumulatedNeverRestartsAndGrowsMonthAfterMonth() {
        loadTheSheet();

        for (Entry entry : BellavistaIncentiveSheet.entries()) {
            if (!entry.document().equals(CARLOS)) {
                continue;
            }
            VendorIncentive carlos = vendor(service().calculateMonth(entry.month()), CARLOS);
            assertThat(carlos.accumulatedTotal()).as("Carlos until %s", entry.month())
                    .isEqualByComparingTo(pesos(entry.accumulated()));
        }
        // Sheet: 12,800 -> 30,700 -> 55,400 -> 66,100 -> 130,500 -> 207,700
        assertThat(vendor(service().calculateMonth(YearMonth.of(2026, 4)), CARLOS).accumulatedTotal())
                .isEqualByComparingTo(pesos(207_700));
    }

    @Test
    void aSellerWhoStartsLaterAccumulatesOnlyFromTheirFirstMonth() {
        loadTheSheet();

        VendorIncentive monica = vendor(service().calculateMonth(YearMonth.of(2026, 4)), MONICA);

        assertThat(monica.accumulatedTotal()).isEqualByComparingTo(monica.monthTotal());
        assertThat(monica.accumulatedTotal()).isEqualByComparingTo(pesos(52_200));
    }

    @Test
    void salesBeforeTheStartOfTheAccumulationAreNotCounted() {
        loadTheSheet();
        sales.with(sale(LocalDate.of(2025, 10, 20), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 500));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2025, 11)), ANA);

        assertThat(ana.accumulatedTotal()).isEqualByComparingTo(pesos(18_800));
    }

    @Test
    void sellersComeSortedByNameAndThenByDocument() {
        loadTheSheet();
        sales.with(sale(LocalDate.of(2026, 4, 3), "0999", "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 1));

        MonthlyIncentives april = service().calculateMonth(YearMonth.of(2026, 4));

        assertThat(april.vendors()).extracting(VendorIncentive::vendorDocument)
                .containsExactly("0999", ANA, CARLOS, MONICA, WILTOM);
    }

    @Test
    void theNameKeptForASellerIsTheOneOfTheirLatestSale() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)));
        sales.with(
                sale(LocalDate.of(2025, 11, 10), ANA, "ANA M. VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 2),
                sale(LocalDate.of(2026, 2, 10), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 3));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.vendorName()).isEqualTo("ANA MILENA VILLAMIL");
    }

    // ---- versioned rules inside a month ----

    @Test
    void aChangeOfValueInsideTheMonthSplitsTheUnitsInTwoLinesWithTheirPeriods() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 10)));
        sales.with(
                sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 10),
                sale(LocalDate.of(2026, 2, 15), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 10));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).extracting(IncentiveLine::units, line -> line.valuePerUnit().intValue(),
                        line -> line.subtotal().intValue(), IncentiveLine::periodStart, IncentiveLine::periodEnd)
                .containsExactlyInAnyOrder(
                        tuple(10, 800, 8_000, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 9)),
                        tuple(10, 1_000, 10_000, LocalDate.of(2026, 2, 10), null));
        assertThat(ana.monthTotal()).isEqualByComparingTo(pesos(18_000));
    }

    @Test
    void theDayTheNewRuleStartsAlreadyUsesItAndTheDayBeforeDoesNot() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 10)));
        sales.with(
                sale(LocalDate.of(2026, 2, 9), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 1),
                sale(LocalDate.of(2026, 2, 10), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 1));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).extracting(line -> line.valuePerUnit().intValue())
                .containsExactlyInAnyOrder(800, 1_000);
    }

    @Test
    void twoChangesInTheSameMonthGiveThreeLinesAndTheMiddleOneIsClosedByTheLast() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 900, LocalDate.of(2026, 2, 10)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 20)));
        sales.with(
                sale(LocalDate.of(2026, 2, 2), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 1),
                sale(LocalDate.of(2026, 2, 15), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 1),
                sale(LocalDate.of(2026, 2, 25), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 1));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).extracting(line -> line.valuePerUnit().intValue(), IncentiveLine::periodStart,
                        IncentiveLine::periodEnd)
                .containsExactlyInAnyOrder(
                        tuple(800, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 9)),
                        tuple(900, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 19)),
                        tuple(1_000, LocalDate.of(2026, 2, 20), null));
        assertThat(ana.monthTotal()).isEqualByComparingTo(pesos(2_700));
    }

    @Test
    void twoRulesWithTheSameValueStayOnSeparateLinesBecauseTheirPeriodsDiffer() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 800, LocalDate.of(2026, 2, 10)));
        sales.with(
                sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 4),
                sale(LocalDate.of(2026, 2, 15), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 6));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).hasSize(2);
        assertThat(ana.monthTotal()).isEqualByComparingTo(pesos(8_000));
    }

    @Test
    void salesOfTheSameRuleOnDifferentDaysAreAddedInOneLine() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)));
        sales.with(
                sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 4),
                sale(LocalDate.of(2026, 2, 15), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 6));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).singleElement().satisfies(line -> {
            assertThat(line.units()).isEqualTo(10);
            assertThat(line.subtotal()).isEqualByComparingTo(pesos(8_000));
        });
    }

    @Test
    void aValueOfZeroPaysNothingButTheUnitsAreStillListed() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 0, LocalDate.of(2026, 2, 1)));
        sales.with(sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 7));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).singleElement().satisfies(line -> {
            assertThat(line.units()).isEqualTo(7);
            assertThat(line.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(ana.unresolved()).isEmpty();
    }

    // ---- units that cannot be valued ----

    @Test
    void unitsOfAnSkuWithoutProductAreReportedAndLeftOutOfTheTotals() {
        rules.with(BellavistaIncentiveSheet.rules());
        sales.with(
                sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 10),
                sale(LocalDate.of(2026, 2, 6), ANA, "ANA MILENA VILLAMIL", "SKU-DESCONOCIDO", "Aditivo viejo", 3));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.monthTotal()).isEqualByComparingTo(pesos(10_000));
        assertThat(ana.accumulatedTotal()).isEqualByComparingTo(pesos(10_000));
        assertThat(ana.unresolved()).containsExactly(
                new UnresolvedAdditive("SKU-DESCONOCIDO", "Aditivo viejo", 3, UnresolvedReason.PRODUCT_NOT_FOUND));
        assertThat(ana.accumulatedUnresolvedUnits()).isEqualTo(3);
    }

    @Test
    void unitsSoldBeforeTheFirstRuleOfTheAdditiveHaveNoRuleInForce() {
        rules.with(rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 10)));
        sales.with(
                sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 4),
                sale(LocalDate.of(2026, 2, 15), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 6));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.monthTotal()).isEqualByComparingTo(pesos(6_000));
        assertThat(ana.unresolved()).containsExactly(
                new UnresolvedAdditive(GASOLINE_SKU, GASOLINE_NAME, 4, UnresolvedReason.NO_RULE_IN_FORCE));
        assertThat(ana.accumulatedUnresolvedUnits()).isEqualTo(4);
        assertThat(ana.lines()).singleElement().satisfies(line -> {
            assertThat(line.periodStart()).isEqualTo(LocalDate.of(2026, 2, 10));
            assertThat(line.periodEnd()).isNull();
        });
    }

    @Test
    void anAdditiveWithoutAnyRuleLeavesAllItsUnitsUnresolvedAndTheSellerStillAppears() {
        sales.with(sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 4));

        MonthlyIncentives february = service().calculateMonth(YearMonth.of(2026, 2));

        VendorIncentive ana = vendor(february, ANA);
        assertThat(ana.lines()).isEmpty();
        assertThat(ana.monthTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ana.unresolved()).extracting(UnresolvedAdditive::reason)
                .containsExactly(UnresolvedReason.NO_RULE_IN_FORCE);
    }

    @Test
    void unresolvedUnitsOfEarlierMonthsCountInTheAccumulatedButAreNotListedInTheMonth() {
        rules.with(BellavistaIncentiveSheet.rules());
        sales.with(
                sale(LocalDate.of(2026, 1, 5), ANA, "ANA MILENA VILLAMIL", "SKU-DESCONOCIDO", "Aditivo viejo", 5),
                sale(LocalDate.of(2026, 2, 5), ANA, "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 10),
                sale(LocalDate.of(2026, 2, 6), ANA, "ANA MILENA VILLAMIL", "SKU-DESCONOCIDO", "Aditivo viejo", 2));

        VendorIncentive january = vendor(service().calculateMonth(YearMonth.of(2026, 1)), ANA);
        VendorIncentive february = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(january.unresolved()).extracting(UnresolvedAdditive::units).containsExactly(5);
        assertThat(january.accumulatedUnresolvedUnits()).isEqualTo(5);
        assertThat(february.unresolved()).extracting(UnresolvedAdditive::units).containsExactly(2);
        // 5 of January + 2 of February: the whole period, this month included
        assertThat(february.accumulatedUnresolvedUnits()).isEqualTo(7);
    }

    @Test
    void unitsUnresolvedInAnEarlierMonthDoNotMakeTheSellerAppearInALaterOne() {
        rules.with(BellavistaIncentiveSheet.rules());
        sales.with(sale(LocalDate.of(2026, 1, 5), ANA, "ANA MILENA VILLAMIL", "SKU-DESCONOCIDO", "Aditivo viejo", 5));

        assertThat(service().calculateMonth(YearMonth.of(2026, 2)).vendors()).isEmpty();
    }

    // ---- months and sellers without sales ----

    @Test
    void aMonthWithoutSalesIsAnEmptyListNotAnError() {
        rules.with(BellavistaIncentiveSheet.rules());

        MonthlyIncentives result = service().calculateMonth(YearMonth.of(2026, 3));

        assertThat(result.vendors()).isEmpty();
        assertThat(result.month()).isEqualTo(YearMonth.of(2026, 3));
        assertThat(result.accumulatedFrom()).isEqualTo(ACCUMULATED_FROM);
    }

    @Test
    void sellersWhoOnlySoldInEarlierMonthsDoNotAppearInTheMonthList() {
        loadTheSheet();
        sales.replaceDay(LocalDate.of(2026, 3, 5), List.of());
        sales.replaceDay(LocalDate.of(2026, 3, 20), List.of());

        assertThat(service().calculateMonth(YearMonth.of(2026, 3)).vendors()).isEmpty();
    }

    @Test
    void aSellerWithoutSalesInTheMonthStillGetsTheAccumulatedOfTheEarlierOnes() {
        loadTheSheet();
        sales.replaceDay(LocalDate.of(2026, 3, 5), List.of());
        sales.replaceDay(LocalDate.of(2026, 3, 20), List.of());

        MonthlyIncentives result = service().calculateVendor(YearMonth.of(2026, 3), ANA);

        assertThat(result.vendors()).singleElement().satisfies(ana -> {
            assertThat(ana.vendorName()).isEqualTo("ANA MILENA VILLAMIL");
            assertThat(ana.lines()).isEmpty();
            assertThat(ana.monthTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(ana.accumulatedTotal()).isEqualByComparingTo(pesos(230_200));
        });
    }

    @Test
    void aSellerWithoutAnySaleGetsZerosAndNoName() {
        loadTheSheet();

        MonthlyIncentives result = service().calculateVendor(YearMonth.of(2026, 2), "  9999 ");

        assertThat(result.vendors()).singleElement().satisfies(nobody -> {
            assertThat(nobody.vendorDocument()).isEqualTo("9999");
            assertThat(nobody.vendorName()).isNull();
            assertThat(nobody.lines()).isEmpty();
            assertThat(nobody.unresolved()).isEmpty();
            assertThat(nobody.monthTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(nobody.accumulatedTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(nobody.accumulatedUnresolvedUnits()).isZero();
        });
    }

    @Test
    void theCalculationOfOneSellerMatchesTheirRowInTheMonthList() {
        loadTheSheet();

        VendorIncentive fromTheList = vendor(service().calculateMonth(YearMonth.of(2026, 3)), WILTOM);
        VendorIncentive alone = service().calculateVendor(YearMonth.of(2026, 3), " " + WILTOM + " ").vendors().get(0);

        assertThat(alone).isEqualTo(fromTheList);
        assertThat(alone.accumulatedTotal()).isEqualByComparingTo(pesos(260_000));
    }

    // ---- which months can be calculated ----

    @Test
    void aMonthAfterTheCurrentOneIsRefusedWithTheLastAvailableMonth() {
        IncentiveException error = catchThrowableOfType(() -> service().calculateMonth(YearMonth.of(2026, 5)), IncentiveException.class);

        assertThat(error.error()).isEqualTo(IncentiveError.MONTH_IN_FUTURE);
        assertThat(error.getMessage()).contains("2026-05").contains("2026-04");
    }

    @Test
    void aMonthBeforeTheStartOfTheAccumulationIsRefused() {
        IncentiveException error = catchThrowableOfType(() -> service().calculateMonth(YearMonth.of(2025, 10)), IncentiveException.class);

        assertThat(error.error()).isEqualTo(IncentiveError.MONTH_BEFORE_START);
        assertThat(error.getMessage()).contains("2025-10").contains("2025-11");
    }

    @Test
    void theCurrentMonthAndTheFirstMonthOfTheAccumulationAreAccepted() {
        loadTheSheet();

        assertThat(service().calculateMonth(YearMonth.of(2026, 4)).vendors()).isNotEmpty();
        assertThat(service().calculateMonth(ACCUMULATED_FROM).vendors()).isNotEmpty();
    }

    @Test
    void theCurrentMonthIsTheOneOfBogotaNotTheOneOfUtc() {
        // 03:00 UTC on May 1st is still 22:00 of April 30th in Bogotá
        IncentiveCalculationService lateApril = serviceAt("2026-05-01T03:00:00Z");
        IncentiveException future = catchThrowableOfType(() -> lateApril.calculateMonth(YearMonth.of(2026, 5)), IncentiveException.class);
        assertThat(future.error()).isEqualTo(IncentiveError.MONTH_IN_FUTURE);
        assertThat(lateApril.calculateMonth(YearMonth.of(2026, 4)).vendors()).isEmpty();

        // 05:00 UTC is midnight in Bogotá: May has started
        assertThat(serviceAt("2026-05-01T05:00:00Z").calculateMonth(YearMonth.of(2026, 5)).vendors()).isEmpty();
    }

    @Test
    void aSingleSellerAlsoValidatesTheMonth() {
        IncentiveException error = catchThrowableOfType(() -> service().calculateVendor(YearMonth.of(2027, 1), ANA), IncentiveException.class);

        assertThat(error.error()).isEqualTo(IncentiveError.MONTH_IN_FUTURE);
    }

    @Test
    void anAdditiveInTheCatalogWithoutSalesChangesNothing() {
        loadTheSheet();
        catalog.with("OTRO-ADITIVO", additive(99L, "Aditivo nuevo", true, 5_000));

        VendorIncentive ana = vendor(service().calculateMonth(YearMonth.of(2026, 2)), ANA);

        assertThat(ana.lines()).hasSize(4);
    }
}
