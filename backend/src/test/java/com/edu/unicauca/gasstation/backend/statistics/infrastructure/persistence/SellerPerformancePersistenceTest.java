package com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.statistics.application.SellerPerformanceService;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.adapters.ShiftSaleHistoryRepositoryAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Availability.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftPeriod.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange.DayStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Isolated PostgreSQL and real Spring application service. Never loads application datasource properties. */
@Testcontainers
class SellerPerformancePersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static HikariDataSource datasource;
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static DataSourceTransactionManager transactionManager;
    private static ShiftSaleHistoryRepository repository;
    private static SellerPerformanceService service;

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class Transactions { }

    @BeforeAll
    static void initializeIsolatedDatabase() {
        var config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(6);
        config.setConnectionTimeout(10000);
        config.setConnectionInitSql("SET lock_timeout = '10s'");
        datasource = new HikariDataSource(config);
        Flyway.configure().dataSource(datasource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(datasource);
        transactionManager = new DataSourceTransactionManager(datasource);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(PlatformTransactionManager.class, () -> transactionManager);
        context.register(Transactions.class, ShiftSaleHistoryRepositoryAdapter.class, SellerPerformanceService.class);
        context.refresh();
        repository = context.getBean(ShiftSaleHistoryRepository.class);
        service = context.getBean(SellerPerformanceService.class);
    }

    @AfterAll
    static void closeResources() {
        if (context != null) context.close();
        if (datasource != null) datasource.close();
    }

    @BeforeEach
    void clearOnlyStatisticsInTheContainer() {
        jdbc.execute("TRUNCATE statistics_shift_sale, statistics_journal_sales RESTART IDENTITY");
    }

    @Test
    void inclusiveRangeKeepsAllFourStatusesAndPreservesEverySnapshotField() {
        var first = sale(A, "1.23456789012345678900", "2.34567890123456789000");
        var second = sale(B, "0.000", "0.00");
        repository.saveJournal(journal(DATE.minusDays(1)), List.of());
        repository.saveJournal(journal(DATE), List.of(first, second));
        repository.saveJournal(journal(DATE.plusDays(1)), List.of());
        repository.markPending(journal(DATE.plusDays(2)), "identity unresolved");
        repository.saveJournal(journal(DATE.plusDays(4)), List.of());

        var result = repository.readRange(DATE, DATE.plusDays(3));

        assertThat(result.days()).extracting(ShiftSaleHistoryRange.Day::status)
                .containsExactly(PROCESSED, PROCESSED_EMPTY, PENDING, ABSENT);
        assertThat(result.sales()).containsExactly(first, second);
        assertThat(result.sales().getFirst().sales()).isEqualTo(first.sales());
        assertThat(result.sales().getFirst().journal().closedAt().getNano()).isEqualTo(999999999);
        assertThat(result.days().get(2).processing().lastError()).isEqualTo("identity unresolved");
        assertThat(result.days().getLast().processing()).isNull();
        assertThat(repository.findByDate(DATE)).containsExactly(first, second);
    }

    @Test
    void aCompletelyAbsentRangeRemainsUnavailableInsteadOfBecomingZeroSales() {
        var range = repository.readRange(DATE, DATE.plusDays(1));
        assertThat(range.days()).extracting(ShiftSaleHistoryRange.Day::status).containsExactly(ABSENT, ABSENT);
        var report = service.compliance(new Window(DATE, DATE.plusDays(1)), FUEL_GALLONS, List.of(), 2);
        assertThat(report.availability()).isEqualTo(UNAVAILABLE);
        assertThat(report.reason()).isEqualTo(Reason.HISTORY_INCOMPLETE);
        assertThat(report.sellers()).isEmpty();
        assertThat(repository.findPending()).isEmpty();
    }

    @Test
    void confirmedEmptyAndRecordedZeroAreDistinctInStorageAndReports() {
        repository.saveJournal(journal(DATE), List.of());
        var empty = service.compliance(SINGLE_DAY, FUEL_GALLONS, List.of(), 2);
        assertThat(empty.availability()).isEqualTo(NO_DATA);
        assertThat(empty.coverage().getFirst().status()).isEqualTo(PROCESSED_EMPTY);
        var tomorrow = DATE.plusDays(1);
        var zero = sale(tomorrow, A, "DIA",
                com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole.TITULAR,
                "0.000", "0.00");
        repository.saveJournal(journal(tomorrow), List.of(zero));
        var window = new Window(tomorrow, tomorrow);
        var observed = service.compliance(window, FUEL_GALLONS,
                List.of(target(A, window, FUEL_GALLONS, "10")), 2);

        assertThat(observed.availability()).isEqualTo(AVAILABLE);
        assertThat(observed.sellers().getFirst().percent()).isZero();
        assertThat(observed.coverage().getFirst().status()).isEqualTo(PROCESSED);
    }

    @Test
    void readsAcrossMonthYearAndOperationalNightBoundariesWithoutMovingClosingDates() {
        var end = LocalDate.of(2026, 12, 31);
        var next = end.plusDays(1);
        var row = sale(end, A, "NOCHE",
                com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole.TITULAR,
                "3.0000", "40.00");
        repository.saveJournal(journal(end), List.of(row));
        repository.saveJournal(journal(next), List.of());

        var range = repository.readRange(end, next);
        assertThat(range.complete()).isTrue();
        assertThat(range.sales()).containsExactly(row);
        assertThat(range.sales().getFirst().journal().journalDate()).isEqualTo(end);
        assertThat(range.sales().getFirst().journal().closedAt().toLocalDate()).isEqualTo(next);
    }

    @Test
    void realStoredAmountsDriveComplianceComparisonAndExplicitNormalization() {
        repository.saveJournal(journal(DATE), List.of(sale(A, "100.12345678901234567890", "10.123"),
                sale(B, "300.00000000000000000000", "20.123")));
        var before = repository.findProcessing(DATE).orElseThrow();
        var snapshots = repository.findByDate(DATE);
        var classes = classified(repository.readRange(DATE, DATE), DAY);

        var compliance = service.compliance(SINGLE_DAY, FUEL_GALLONS,
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "100"), target(B, SINGLE_DAY, FUEL_GALLONS, "200")), 8);
        var comparison = service.compare(SINGLE_DAY, FUEL_GALLONS, classes, 8);
        var normalized = service.normalize(SINGLE_DAY, BASKET_COP, classes,
                List.of(new ShiftMultiplier(SINGLE_DAY, BASKET_COP, DAY, new BigDecimal("1.123456789"),
                        "explicit multiplier fixture")), 8);

        assertThat(compliance.sellers()).extracting(Compliance::workerId).containsExactly(B, A);
        assertThat(compliance.sellers().getLast().sales()).isEqualByComparingTo("100.12345678901234567890");
        assertThat(comparison.sellers().getFirst().stationObservations()).isEqualTo(2);
        assertThat(normalized.sellers().getFirst().normalizedSales())
                .isEqualTo(new BigDecimal("10.123").multiply(new BigDecimal("1.123456789")));
        assertThat(repository.findProcessing(DATE)).contains(before);
        assertThat(repository.findByDate(DATE)).isEqualTo(snapshots);
    }

    @Test
    void missingRulesAndPendingDatesAreUnavailableWithoutInventingPeriodTargets() {
        repository.saveJournal(journal(DATE), List.of(sale(A, "100", "10")));
        var noTarget = service.compliance(SINGLE_DAY, FUEL_GALLONS, List.of(), 2);
        var noPeriod = service.compare(SINGLE_DAY, FUEL_GALLONS, List.of(), 2);
        assertThat(noTarget.sellers().getFirst().reason()).isEqualTo(Reason.TARGET_NOT_PROVIDED);
        assertThat(noPeriod.reason()).isEqualTo(Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED);
        // The automatic report resolves an absent optional producer in the real Spring context.
        var automatic = service.report(SINGLE_DAY, FUEL_GALLONS, 2);
        assertThat(automatic.compliance().sellers().getFirst().sales()).isEqualByComparingTo("100");
        assertThat(automatic.compliance().sellers().getFirst().reason()).isEqualTo(Reason.TARGET_NOT_PROVIDED);
        assertThat(automatic.comparison().reason()).isEqualTo(Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED);
        assertThat(automatic.normalization().reason()).isEqualTo(Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED);
        assertThat(repository.findByDate(DATE)).hasSize(1);
        repository.markPending(journal(DATE.plusDays(1)), "worker unavailable");
        var incomplete = service.compliance(new Window(DATE, DATE.plusDays(1)), FUEL_GALLONS, List.of(), 2);
        assertThat(incomplete.reason()).isEqualTo(Reason.HISTORY_INCOMPLETE);
        assertThat(incomplete.sellers()).isEmpty();
    }

    @Test
    void noUnconfirmedRowsAreExposedEvenIfTheDatabaseContainsAnUnconfirmedPrefix() {
        repository.saveJournal(journal(DATE), List.of(sale(A, "100", "10")));
        jdbc.update("UPDATE statistics_journal_sales SET status='PENDING', sale_count=NULL, processed_at=NULL, "
                + "last_error='simulated unconfirmed prefix' WHERE journal_date=?", DATE);

        var range = repository.readRange(DATE, DATE);
        assertThat(range.days().getFirst().status()).isEqualTo(PENDING);
        assertThat(range.sales()).isEmpty();
    }

    @Test
    void inconsistentConfirmedCountsAreRejectedInsteadOfReturningPartialHistory() {
        repository.saveJournal(journal(DATE), List.of(sale(A, "100", "10")));
        jdbc.update("UPDATE statistics_journal_sales SET sale_count=2 WHERE journal_date=?", DATE);
        assertThatThrownBy(() -> repository.readRange(DATE, DATE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rangeReadsSeeEitherThePendingDateOrItsWholeCommittedBatchDuringAConcurrentWrite() throws Exception {
        repository.saveJournal(journal(DATE), List.of(sale(A, "100", "10")));
        var tomorrow = DATE.plusDays(1);
        repository.markPending(journal(tomorrow), "retry outstanding");
        var staged = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var writer = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                copySaleAndConfirm(tomorrow);
                staged.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("test release timed out");
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
            }));
            try {
                assertThat(staged.await(10, TimeUnit.SECONDS)).isTrue();
                var during = repository.readRange(tomorrow, tomorrow);
                assertThat(during.days().getFirst().status()).isEqualTo(PENDING);
                assertThat(during.sales()).isEmpty();
                release.countDown();
                writer.get(20, TimeUnit.SECONDS);
                var after = repository.readRange(tomorrow, tomorrow);
                assertThat(after.days().getFirst().status()).isEqualTo(PROCESSED);
                assertThat(after.sales()).hasSize(1);
                assertThat(after.days().getFirst().processing().saleCount()).isEqualTo(after.sales().size());
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void aRolledBackConcurrentBatchDoesNotAppearInTheRange() {
        repository.saveJournal(journal(DATE), List.of(sale(A, "100", "10")));
        var tomorrow = DATE.plusDays(1);
        repository.markPending(journal(tomorrow), "retry outstanding");

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            copySaleAndConfirm(tomorrow);
            throw new IllegalStateException("rollback batch");
        })).hasMessage("rollback batch");
        assertThat(repository.readRange(tomorrow, tomorrow).days().getFirst().status()).isEqualTo(PENDING);
        assertThat(repository.readRange(tomorrow, tomorrow).sales()).isEmpty();
    }

    private static void copySaleAndConfirm(LocalDate targetDate) {
        var columns = "vendor_document, shift_name, worker_id, vendor_name, worked_code, effective_role, support, "
                + "motor_gallons, motor_gallons_scale, diesel_gallons, diesel_gallons_scale, extra_gallons, "
                + "extra_gallons_scale, maxpro_gallons, maxpro_gallons_scale, basket_dominus_amount_cop, "
                + "basket_dominus_amount_cop_scale, dominus_tirilla_cop, dominus_tirilla_cop_scale, "
                + "total_delivered_cop, total_delivered_cop_scale";
        jdbc.update("INSERT INTO statistics_shift_sale (journal_date, " + columns + ") SELECT ?, "
                + columns + " FROM statistics_shift_sale WHERE journal_date=?", targetDate, DATE);
        jdbc.update("UPDATE statistics_journal_sales SET status='PROCESSED', sale_count=1, processed_at=now(), "
                + "attempts=attempts+1, last_error=NULL WHERE journal_date=?", targetDate);
    }
}
