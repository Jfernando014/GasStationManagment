package com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.adapters.ShiftSaleHistoryRepositoryAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing.Status.PENDING;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing.Status.PROCESSED;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole.APOYO;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole.TITULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/** Real PostgreSQL, real commits and real row locks; never loads application datasource configuration. */
@Testcontainers
class ShiftSaleHistoryPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);
    private static final UUID WORKER_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID WORKER_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final String SALE_COLUMNS = "journal_date, vendor_document, shift_name, worker_id, vendor_name, worked_code, effective_role, support, motor_gallons, motor_gallons_scale, diesel_gallons, diesel_gallons_scale, extra_gallons, extra_gallons_scale, maxpro_gallons, maxpro_gallons_scale, basket_dominus_amount_cop, basket_dominus_amount_cop_scale, dominus_tirilla_cop, dominus_tirilla_cop_scale, total_delivered_cop, total_delivered_cop_scale";
    private static HikariDataSource datasource;
    private static JdbcTemplate jdbc;
    private static DataSourceTransactionManager transactions;
    private static ShiftSaleHistoryRepository repository;

    @BeforeAll
    static void initializeIsolatedDatabase() {
        var config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10000);
        config.setConnectionInitSql("SET lock_timeout = '10s'");
        datasource = new HikariDataSource(config);
        Flyway.configure().dataSource(datasource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(datasource);
        transactions = new DataSourceTransactionManager(datasource);
        repository = new ShiftSaleHistoryRepositoryAdapter(jdbc, transactions);
    }

    @AfterAll
    static void closePool() {
        if (datasource != null) {
            datasource.close();
        }
    }

    @BeforeEach
    void clearStatisticsOnly() {
        jdbc.execute("TRUNCATE TABLE statistics_shift_sale, statistics_journal_sales RESTART IDENTITY");
    }

    @Test
    void roundTripsEveryFieldAcrossSeveralVendorsAndShifts() {
        var journal = journal(DATE);
        var first = sale(journal, "001001", WORKER_A, "12-7", APOYO, preciseAmounts());
        var second = sale(journal, "001001", WORKER_A, "NOCHE", TITULAR, values("0.000", "0.00"));
        var third = sale(journal, WORKER_B.toString(), WORKER_B, "DIA", TITULAR, values("100.123", "900.99"));

        var result = repository.saveJournal(journal, List.of(first, second, third));

        assertThat(result.status()).isEqualTo(PROCESSED);
        assertThat(result.journal()).isEqualTo(journal);
        assertThat(result.saleCount()).isEqualTo(3);
        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.processedAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();
        assertThat(result.lastError()).isNull();
        assertThat(repository.findByDate(DATE)).containsExactlyInAnyOrder(first, second, third);
        assertThat(repository.findByIdentity(first.identity())).contains(first);
        assertThat(repository.findByIdentity(third.identity()).orElseThrow().identity().vendorDocument()).hasSize(36);
        assertThat(jdbc.queryForList("SELECT support FROM statistics_shift_sale ORDER BY vendor_document, shift_name",
                Boolean.class)).containsExactly(false, true, false);
    }

    @Test
    void unknownDaysAndKeysRemainMissing() {
        assertThat(repository.findProcessing(DATE)).isEmpty();
        assertThat(repository.findByDate(DATE)).isEmpty();
        assertThat(repository.findByIdentity(new ShiftSaleIdentity(DATE, "missing", "DIA"))).isEmpty();
        assertThat(repository.findPending()).isEmpty();
    }

    @Test
    void storesAnnouncementsWithoutDatabaseJournalId() {
        var original = journal(DATE);
        var journal = new ClosedJournalSnapshot(null, DATE, original.consecutive(), original.closedAt());
        var snapshot = sale(journal, "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());

        repository.saveJournal(journal, List.of(snapshot));

        assertThat(repository.findProcessing(DATE).orElseThrow().journal()).isEqualTo(journal);
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
    }

    @Test
    void retainsOriginalTextInsteadOfNormalizingHistoricalKeysOrDescriptions() {
        var snapshot = new ShiftSaleHistory(journal(DATE), new ShiftSaleIdentity(DATE, " 001001 ", " DIA "),
                WORKER_A, " Ana Histórica ", "DIA", TITULAR, preciseAmounts());

        repository.saveJournal(snapshot.journal(), List.of(snapshot));

        assertThat(repository.findByIdentity(snapshot.identity())).contains(snapshot);
        assertThat(repository.findByIdentity(new ShiftSaleIdentity(DATE, "001001", "DIA"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00000", "1E+3", "0E+5", "-7E+2", "-0.000000000000000000001",
            "999999999999999999999999999999.123456789012345678900", "1E-16383"})
    void preservesDecimalValuesAndScalesIncludingZeroSignedAndScientificValues(String value) {
        var decimal = new BigDecimal(value);
        var amounts = new ShiftSaleAmounts(decimal, decimal, decimal, decimal, decimal, decimal, decimal);
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, amounts);

        repository.saveJournal(snapshot.journal(), List.of(snapshot));

        assertThat(repository.findByIdentity(snapshot.identity())).contains(snapshot);
    }

    @Test
    void processesEmptyDaysWithoutFabricatingZeroSalesAndNeverAddsRowsOnRepeat() {
        var journal = journal(DATE);
        var processed = repository.saveJournal(journal, List.of());

        assertThat(processed.status()).isEqualTo(PROCESSED);
        assertThat(processed.saleCount()).isZero();
        assertThat(repository.findByDate(DATE)).isEmpty();
        var later = sale(journal, "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        assertThat(repository.saveJournal(journal, List.of(later))).isEqualTo(processed);
        assertThat(repository.findByDate(DATE)).isEmpty();
    }

    @Test
    void repeatedProcessingCannotReplaceConfirmedMetadataRolesNamesOrAmounts() {
        var originalJournal = journal(DATE);
        var original = sale(originalJournal, "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        var stored = repository.saveJournal(originalJournal, List.of(original));
        var changedJournal = new ClosedJournalSnapshot(99L, DATE, 999L, originalJournal.closedAt().plusDays(5));
        var changed = new ShiftSaleHistory(changedJournal, original.identity(), WORKER_B, "Changed", "12-7",
                APOYO, values("1", "2"));

        assertThat(repository.saveJournal(changedJournal, List.of(changed))).isEqualTo(stored);
        assertThat(repository.findByDate(DATE)).containsExactly(original);
    }

    @Test
    void databaseEnforcesBusinessUniquenessWithoutAnExistsCheck() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        repository.saveJournal(snapshot.journal(), List.of(snapshot));

        assertThatThrownBy(() -> jdbc.execute("INSERT INTO statistics_shift_sale (" + SALE_COLUMNS + ") SELECT "
                + SALE_COLUMNS + " FROM statistics_shift_sale"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_statistics_shift_sale_identity");
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
    }

    @Test
    void failedBatchRollsBackAllSalesAndRetainsARecoverablePendingDay() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());

        assertThatThrownBy(() -> repository.saveJournal(snapshot.journal(), List.of(snapshot, snapshot)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM statistics_shift_sale", Integer.class)).isZero();
        var pending = repository.findProcessing(DATE).orElseThrow();
        assertThat(pending.status()).isEqualTo(PENDING);
        assertThat(pending.attempts()).isEqualTo(1);
        assertThat(pending.saleCount()).isNull();
        assertThat(pending.processedAt()).isNull();
        assertThat(pending.lastError()).isNotBlank();
        assertThat(repository.findPending()).containsExactly(pending);

        var retried = repository.saveJournal(snapshot.journal(), List.of(snapshot));
        assertThat(retried.status()).isEqualTo(PROCESSED);
        assertThat(retried.attempts()).isEqualTo(2);
        assertThat(retried.lastError()).isNull();
        assertThat(repository.findPending()).isEmpty();
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E-16384", "1E+131072", "0E+131072", "1E-2147483647", "1E+2147483647"})
    void unsupportedPrecisionFailsWithoutRoundingOrLeavingPartialSales(String value) {
        var journal = journal(DATE);
        var valid = sale(journal, "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        var invalid = sale(journal, "001002", WORKER_B, "DIA", TITULAR, values(value, "1.00"));

        assertThatThrownBy(() -> repository.saveJournal(journal, List.of(valid, invalid)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exact PostgreSQL NUMERIC capacity");
        assertThat(repository.findByDate(DATE)).isEmpty();
        assertThat(repository.findProcessing(DATE).orElseThrow().status()).isEqualTo(PENDING);
    }

    @Test
    void mismatchedJournalMetadataRollsBackTheWholeBatch() {
        var journal = journal(DATE);
        var valid = sale(journal, "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        var differentAnnouncement = new ClosedJournalSnapshot(99L, DATE, 105L, journal.closedAt());
        var invalid = sale(differentAnnouncement, "001002", WORKER_B, "NOCHE", TITULAR, preciseAmounts());

        assertThatThrownBy(() -> repository.saveJournal(journal, List.of(valid, invalid)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("same closed journal");
        assertThat(repository.findByDate(DATE)).isEmpty();
        assertThat(repository.findProcessing(DATE).orElseThrow().status()).isEqualTo(PENDING);
    }

    @Test
    void missingBatchIsPendingRatherThanAnEmptySuccessfulDay() {
        assertThatThrownBy(() -> repository.saveJournal(journal(DATE), null))
                .isInstanceOf(NullPointerException.class);
        assertThat(repository.findProcessing(DATE).orElseThrow().status()).isEqualTo(PENDING);
        assertThat(repository.findByDate(DATE)).isEmpty();
    }

    @Test
    void pendingErrorsAreBoundedAndPendingDaysAreListedInOperationalOrder() {
        repository.markPending(journal(DATE.plusDays(1)), "unresolved worker");
        repository.markPending(journal(DATE), "first failure");
        var pending = repository.markPending(journal(DATE), "x".repeat(900));

        assertThat(pending.status()).isEqualTo(PENDING);
        assertThat(pending.attempts()).isEqualTo(2);
        assertThat(pending.lastError()).hasSize(500);
        assertThat(repository.findPending()).extracting(p -> p.journal().journalDate())
                .containsExactly(DATE, DATE.plusDays(1));
    }

    @Test
    void lateFailureNeverDowngradesOrRewritesAConfirmedDay() {
        var journal = journal(DATE);
        var snapshot = sale(journal, "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        var processed = repository.saveJournal(journal, List.of(snapshot));

        assertThat(repository.markPending(journal, "late failure")).isEqualTo(processed);
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
        assertThat(repository.findPending()).isEmpty();
    }

    @Test
    void saveCommitsIndependentlyOfACallerRollback() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());

        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(transaction -> {
            repository.saveJournal(snapshot.journal(), List.of(snapshot));
            throw new IllegalStateException("caller failed after storage committed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
        assertThat(repository.findProcessing(DATE).orElseThrow().status()).isEqualTo(PROCESSED);
    }

    @Test
    void pendingMarkerSurvivesBothTheFailedBatchAndACallerRollback() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());

        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(transaction ->
                repository.saveJournal(snapshot.journal(), List.of(snapshot, snapshot))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.findByDate(DATE)).isEmpty();
        assertThat(repository.findProcessing(DATE).orElseThrow().status()).isEqualTo(PENDING);
    }

    @Test
    void concurrentFirstInsertStoresOneCompleteSnapshotWithoutMixedOrDuplicateRows() throws Exception {
        var journal = journal(DATE);
        var firstBatch = List.of(sale(journal, "001001", WORKER_A, "DIA", TITULAR, values("10.123", "100.01")),
                sale(journal, "001002", WORKER_B, "12-7", APOYO, values("20.123", "200.01")));
        var secondBatch = List.of(sale(journal, "001001", WORKER_A, "DIA", TITULAR, values("30.456", "300.02")),
                sale(journal, "001002", WORKER_B, "12-7", APOYO, values("40.456", "400.02")));
        var barrier = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return repository.saveJournal(journal, firstBatch);
            });
            var second = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return repository.saveJournal(journal, secondBatch);
            });
            assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(second.get(30, TimeUnit.SECONDS));
        }

        var stored = repository.findByDate(DATE);
        assertThat(stored.equals(firstBatch) || stored.equals(secondBatch)).isTrue();
        assertThat(repository.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(1);
    }

    @Test
    void existingPendingRowSerializesWritersWhileAnotherDateCanCommit() throws Exception {
        var journal = journal(DATE);
        repository.markPending(journal, "retry needed");
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var holder = executor.submit(() -> new TransactionTemplate(transactions).execute(transaction -> {
                jdbc.queryForList("SELECT journal_date FROM statistics_journal_sales WHERE journal_date = ? FOR UPDATE",
                        Date.valueOf(DATE));
                locked.countDown();
                try {
                    if (!release.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting for test release");
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
                return null;
            }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                var waiter = executor.submit(() -> repository.saveJournal(journal, List.of()));
                await().atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                        assertThat(jdbc.queryForObject("""
                                SELECT count(*) FROM pg_stat_activity
                                WHERE wait_event_type = 'Lock' AND query LIKE '%statistics_journal_sales%'
                                """, Integer.class)).isGreaterThan(0));
                assertThat(waiter.isDone()).isFalse();
                assertThat(repository.saveJournal(journal(DATE.plusDays(1)), List.of()).status()).isEqualTo(PROCESSED);
                release.countDown();
                holder.get(10, TimeUnit.SECONDS);
                assertThat(waiter.get(15, TimeUnit.SECONDS).status()).isEqualTo(PROCESSED);
            } finally {
                release.countDown();
            }
        }
        assertThat(repository.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(2);
    }

    @Test
    void concurrentFailureMarkerCannotDowngradeSuccessfulProcessing() throws Exception {
        var journal = journal(DATE);
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var failure = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return repository.markPending(journal, "upstream failed");
            });
            var success = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return repository.saveJournal(journal, List.of());
            });
            failure.get(30, TimeUnit.SECONDS);
            assertThat(success.get(30, TimeUnit.SECONDS).status()).isEqualTo(PROCESSED);
        }
        assertThat(repository.findProcessing(DATE).orElseThrow().status()).isEqualTo(PROCESSED);
        assertThat(repository.findPending()).isEmpty();
    }

    @Test
    void databaseRejectsInvalidRolesSupportAndProcessingStates() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "12-7", APOYO, preciseAmounts());
        repository.saveJournal(snapshot.journal(), List.of(snapshot));

        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_shift_sale SET support = false"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_shift_sale SET effective_role = 'OTHER'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_shift_sale SET worker_id = ?",
                new UUID(0, 0))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_journal_sales SET sale_count = NULL"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_journal_sales SET last_error = 'failure'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
    void databaseRejectsNonfiniteNumericValues(String invalid) {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        repository.saveJournal(snapshot.journal(), List.of(snapshot));

        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_shift_sale SET motor_gallons = CAST(? AS numeric)", invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
    }

    @Test
    void databaseRejectsScaleMetadataThatWouldLoseDigits() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        repository.saveJournal(snapshot.journal(), List.of(snapshot));

        assertThatThrownBy(() -> jdbc.update("UPDATE statistics_shift_sale SET motor_gallons_scale = 2"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findByDate(DATE)).containsExactly(snapshot);
    }

    @Test
    void databaseRequiresAnOwningStatisticsJournal() {
        var snapshot = sale(journal(DATE), "001001", WORKER_A, "DIA", TITULAR, preciseAmounts());
        repository.saveJournal(snapshot.journal(), List.of(snapshot));
        String selected = SALE_COLUMNS.replace("journal_date", "journal_date + 1");

        assertThatThrownBy(() -> jdbc.execute("INSERT INTO statistics_shift_sale (" + SALE_COLUMNS + ") SELECT "
                + selected + " FROM statistics_shift_sale")).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static ClosedJournalSnapshot journal(LocalDate date) {
        return new ClosedJournalSnapshot(15L, date, 105L, date.plusDays(1).atTime(6, 15, 30, 999999999));
    }

    private static ShiftSaleHistory sale(ClosedJournalSnapshot journal, String document, UUID worker, String shift,
            ShiftSaleHistory.EffectiveRole role, ShiftSaleAmounts amounts) {
        return new ShiftSaleHistory(journal, new ShiftSaleIdentity(journal.journalDate(), document, shift),
                worker, "Ana Histórica " + document, shift, role, amounts);
    }

    private static ShiftSaleAmounts values(String gallons, String cop) {
        var volume = new BigDecimal(gallons);
        var money = new BigDecimal(cop);
        return new ShiftSaleAmounts(volume, volume, volume, volume, money, money, money);
    }

    private static ShiftSaleAmounts preciseAmounts() {
        return new ShiftSaleAmounts(new BigDecimal("26920.123456789012345678900"), new BigDecimal("1.000"),
                new BigDecimal("-0.000000000000000000001"), new BigDecimal("42.120000"),
                new BigDecimal("8400000.123456789012345678900"), new BigDecimal("9000000.0000"),
                new BigDecimal("8999999.990000"));
    }
}
