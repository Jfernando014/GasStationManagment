package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyDayRow;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliation;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliationCalculator;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Monthly C-05 queries against disposable PostgreSQL; the schema is migrated only to V25. */
@Testcontainers
class QrPointsMonthlyQueriesTest {

    private static final LocalDate FIRST = LocalDate.of(2026, 8, 1);
    private static final LocalDate LAST = LocalDate.of(2026, 8, 31);
    private static final LocalDate DAY5 = LocalDate.of(2026, 8, 5);
    private static final LocalDate DAY6 = LocalDate.of(2026, 8, 6);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static QrPointsRepositoryAdapter adapter;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("25").load().migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()));
        adapter = new QrPointsRepositoryAdapter(jdbc);
    }

    @BeforeEach
    void emptyTables() {
        jdbc.update("DELETE FROM redeban_qr_transaction");
        jdbc.update("DELETE FROM points_colombia_day");
    }

    private static void redeban(LocalDate date, String amount, int row) {
        jdbc.update("""
                INSERT INTO redeban_qr_transaction (business_date, tx_time, amount, reference, source_row, worker_id,
                    assignment_status) VALUES (?, NULL, ?, NULL, ?, NULL, 'NO_TIME')
                """, Date.valueOf(date), new BigDecimal(amount), row);
    }

    private static void points(LocalDate date, String total) {
        jdbc.update("INSERT INTO points_colombia_day (business_date, points_total, source) VALUES (?, ?, 'MANUAL')",
                Date.valueOf(date), new BigDecimal(total));
    }

    @Test
    void redebanRowsAreSummedPerDayInsideTheRangeOnly() {
        redeban(DAY5, "500000.00", 1);
        redeban(DAY5, "376008.00", 2);
        redeban(DAY6, "100.50", 1);
        redeban(LocalDate.of(2026, 7, 31), "999.00", 1);
        redeban(LocalDate.of(2026, 9, 1), "888.00", 1);

        Map<LocalDate, BigDecimal> totals = adapter.findRedebanTotals(FIRST, LAST);

        assertThat(totals).containsOnlyKeys(DAY5, DAY6);
        assertThat(totals.get(DAY5)).isEqualByComparingTo("876008.00");
        assertThat(totals.get(DAY6)).isEqualByComparingTo("100.50");
    }

    @Test
    void theRangeBoundsAreInclusive() {
        redeban(FIRST, "10.00", 1);
        redeban(LAST, "20.00", 1);

        assertThat(adapter.findRedebanTotals(FIRST, LAST)).containsOnlyKeys(FIRST, LAST);
    }

    @Test
    void pointsTotalsAreReadPerDayInsideTheRangeOnly() {
        points(FIRST, "46050.00");
        points(DAY5, "41380.00");
        points(LocalDate.of(2026, 7, 31), "1.00");
        points(LocalDate.of(2026, 9, 1), "2.00");

        Map<LocalDate, BigDecimal> totals = adapter.findPointsTotals(FIRST, LAST);

        assertThat(totals).containsOnlyKeys(FIRST, DAY5);
        assertThat(totals.get(FIRST)).isEqualByComparingTo("46050.00");
        assertThat(totals.get(DAY5)).isEqualByComparingTo("41380.00");
    }

    @Test
    void anEmptyOrInvertedRangeGivesNothing() {
        redeban(DAY5, "1.00", 1);
        points(DAY5, "1.00");

        assertThat(adapter.findRedebanTotals(LAST, FIRST)).isEmpty();
        assertThat(adapter.findPointsTotals(LAST, FIRST)).isEmpty();
        assertThat(adapter.findRedebanTotals(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).isEmpty();
        assertThat(adapter.findPointsTotals(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).isEmpty();
    }

    @Test
    void whatTheDatabaseReturnsFeedsTheMonthlyTableWithTwoDecimals() {
        redeban(DAY5, "500000.00", 1);
        redeban(DAY5, "376008.00", 2);
        points(FIRST, "46050.00");
        points(DAY5, "41380.00");

        MonthlyReconciliation result = MonthlyReconciliationCalculator.calculate(YearMonth.of(2026, 8), Map.of(),
                adapter.findRedebanTotals(FIRST, LAST), adapter.findPointsTotals(FIRST, LAST),
                new BigDecimal("0.026500"));

        MonthlyDayRow day5 = result.days().get(4);
        assertThat(day5.qrSource()).isEqualTo(MonthlyDayRow.QrSource.REDEBAN);
        assertThat(day5.qr()).isEqualTo(new BigDecimal("876008.00"));
        assertThat(result.totals().points()).isEqualTo(new BigDecimal("87430.00"));
        assertThat(result.daysWithData()).isEqualTo(2);
    }
}
