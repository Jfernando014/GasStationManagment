package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException;
import com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.adapters.ShiftSaleHistoryRepositoryAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing.Status.PENDING;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing.Status.PROCESSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Spring-managed use case, public source mocks, real PostgreSQL and the unchanged increment 2 adapter. */
@Testcontainers
class ShiftSaleHistoryRegistrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static HikariDataSource datasource;
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static PlatformTransactionManager transactions;
    private static SellerCloseQuery sales;
    private static DayStaffQuery staff;
    private static ShiftSaleHistoryRepository history;
    private static ShiftSaleHistoryRegistrationService service;

    @BeforeAll
    static void initializeIsolatedDatabaseAndSpringService() {
        var config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(8);
        config.setConnectionTimeout(10000);
        config.setConnectionInitSql("SET lock_timeout = '10s'");
        datasource = new HikariDataSource(config);
        Flyway.configure().dataSource(datasource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(datasource);
        transactions = new DataSourceTransactionManager(datasource);
        sales = mock(SellerCloseQuery.class);
        staff = mock(DayStaffQuery.class);

        context = new AnnotationConfigApplicationContext();
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(PlatformTransactionManager.class, () -> transactions);
        context.registerBean(SellerCloseQuery.class, () -> sales);
        context.registerBean(DayStaffQuery.class, () -> staff);
        context.register(Transactions.class, ShiftSaleHistoryRepositoryAdapter.class, ShiftSaleHistoryRegistrationService.class);
        context.refresh();
        history = context.getBean(ShiftSaleHistoryRepository.class);
        service = context.getBean(ShiftSaleHistoryRegistrationService.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class Transactions {
    }

    @AfterAll
    static void closeResources() {
        if (context != null) {
            context.close();
        }
        if (datasource != null) {
            datasource.close();
        }
    }

    @BeforeEach
    void clearOnlyStatisticsAndResetSourceMocks() {
        jdbc.execute("TRUNCATE TABLE statistics_shift_sale, statistics_journal_sales RESTART IDENTITY");
        reset(sales, staff);
    }

    @Test
    void publicQueriesProduceCompleteStoredSnapshotsWithExactValuesAndEffectiveSupport() {
        givenTwoValidSellers();

        var result = service.registerClosedJournal(JOURNAL);

        assertThat(result.status()).isEqualTo(PROCESSED);
        assertThat(result.saleCount()).isEqualTo(2);
        var rows = history.findByDate(DATE);
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().journal()).isEqualTo(JOURNAL);
        assertThat(rows.getFirst().identity().vendorDocument()).isEqualTo(WORKER_A.toString()).hasSize(36);
        assertThat(rows.getFirst().vendorName()).isEqualTo("Recorded seller");
        assertThat(rows.getFirst().sales()).isEqualTo(AMOUNTS);
        assertThat(rows.getFirst().support()).isFalse();
        assertThat(rows.getLast().support()).isTrue();
        assertThat(rows.getLast().workedCode()).isEqualTo("12-7");
        verify(sales, times(1)).findVendorClosesByDate(DATE);
        verify(staff, times(1)).getDayStaff(DATE);
    }

    @Test
    void unresolvedSellerPreventsPartialHistoryAndCanBeRetriedWhenTheSourceProvidesTheWorker() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close(WORKER_B, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL))
                .isInstanceOf(ShiftSaleHistoryIntegrationException.class);
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM statistics_shift_sale", Integer.class)).isZero();
        var pending = history.findProcessing(DATE).orElseThrow();
        assertThat(pending.status()).isEqualTo(PENDING);
        assertThat(pending.attempts()).isEqualTo(1);
        assertThat(pending.lastError()).contains("WORKER_NOT_IN_DAY_STAFF");

        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));
        var retried = service.registerClosedJournal(JOURNAL);

        assertThat(retried.status()).isEqualTo(PROCESSED);
        assertThat(retried.attempts()).isEqualTo(2);
        assertThat(history.findByDate(DATE)).hasSize(2);
        assertThat(history.findPending()).isEmpty();
    }

    @Test
    void aCedulaIsRecordedAsAnIncidentWithoutCreatingAnyRows() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close("001001", "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));

        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL))
                .isInstanceOf(ShiftSaleHistoryIntegrationException.class);
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(history.findProcessing(DATE).orElseThrow().lastError()).contains("INVALID_VENDOR_IDENTIFIER");
    }

    @Test
    void emptyAvailableSalesCreateAConfirmedEmptyDayWithoutAStaffLookup() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of());

        var result = service.registerClosedJournal(JOURNAL);

        assertThat(result.status()).isEqualTo(PROCESSED);
        assertThat(result.saleCount()).isZero();
        assertThat(history.findByDate(DATE)).isEmpty();
        verifyNoInteractions(staff);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sales", "staff"})
    void aSourceFailureStaysPendingUntilARetrySucceeds(String source) {
        var failure = new IllegalStateException(source + " offline");
        if ("sales".equals(source)) {
            when(sales.findVendorClosesByDate(DATE)).thenThrow(failure);
        } else {
            when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
            when(staff.getDayStaff(DATE)).thenThrow(failure);
        }

        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL)).isSameAs(failure);
        var pending = history.findProcessing(DATE).orElseThrow();
        assertThat(pending.status()).isEqualTo(PENDING);
        assertThat(pending.saleCount()).isNull();
        assertThat(pending.lastError()).contains(source + " offline");
        assertThat(history.findByDate(DATE)).isEmpty();

        reset(sales, staff);
        givenTwoValidSellers();
        assertThat(service.registerClosedJournal(JOURNAL).attempts()).isEqualTo(2);
        assertThat(history.findByDate(DATE)).hasSize(2);
    }

    @Test
    void confirmedHistorySurvivesLaterWorkerDeactivationScheduleChangesOrSourceOutage() {
        givenTwoValidSellers();
        var processed = service.registerClosedJournal(JOURNAL);
        var stored = history.findByDate(DATE);
        reset(sales, staff);

        assertThat(service.registerClosedJournal(JOURNAL)).isEqualTo(processed);

        assertThat(history.findByDate(DATE)).isEqualTo(stored);
        verifyNoInteractions(sales, staff);
    }

    @Test
    void storageFailureIsCountedOnceAndDoesNotLeaveAStoredPrefixOfTheDay() {
        var tooPrecise = new ShiftSaleAmounts(new BigDecimal("1E-16384"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"),
                close(DATE, WORKER_B.toString(), "12-7", "Seller B", 3, tooPrecise)));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));

        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL)).isInstanceOf(IllegalArgumentException.class);
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(history.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(1);
        assertThat(history.findProcessing(DATE).orElseThrow().status()).isEqualTo(PENDING);
    }

    @Test
    void theSpringServiceSuspendsCallerTransactionsDuringSourceReadsAndStorageRemainsCommitted() {
        when(sales.findVendorClosesByDate(DATE)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(close(WORKER_A, "DIA"));
        });
        when(staff.getDayStaff(DATE)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return day(member(WORKER_A, "DIA", "TITULAR"));
        });

        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(transaction -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            service.registerClosedJournal(JOURNAL);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            throw new IllegalStateException("caller rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("caller rollback");

        assertThat(history.findProcessing(DATE).orElseThrow().status()).isEqualTo(PROCESSED);
        assertThat(history.findByDate(DATE)).hasSize(1);
    }

    @Test
    void concurrentServiceCallsKeepOneConfirmedSnapshotAndOneSuccessfulAttempt() throws Exception {
        var barrier = new CyclicBarrier(2);
        when(sales.findVendorClosesByDate(DATE)).thenAnswer(invocation -> {
            barrier.await(10, TimeUnit.SECONDS);
            return List.of(close(WORKER_A, "DIA"));
        });
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.registerClosedJournal(JOURNAL));
            var second = executor.submit(() -> service.registerClosedJournal(JOURNAL));
            assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(second.get(30, TimeUnit.SECONDS));
        }

        assertThat(history.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(1);
        assertThat(history.findByDate(DATE)).hasSize(1);
    }

    @Test
    void anotherCommittedSnapshotWinsOverASourceFailureThatArrivesLater() throws Exception {
        var reading = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var failure = new IllegalStateException("source failed after another registration");
        when(sales.findVendorClosesByDate(DATE)).thenAnswer(invocation -> {
            reading.countDown();
            if (!release.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test release timed out");
            }
            throw failure;
        });

        try (var executor = Executors.newSingleThreadExecutor()) {
            var delayed = executor.submit(() -> service.registerClosedJournal(JOURNAL));
            try {
                assertThat(reading.await(10, TimeUnit.SECONDS)).isTrue();
                var snapshot = new ShiftSaleHistory(JOURNAL, new ShiftSaleIdentity(DATE, WORKER_A.toString(), "DIA"),
                        WORKER_A, "Confirmed elsewhere", "DIA", ShiftSaleHistory.EffectiveRole.TITULAR, AMOUNTS);
                var processed = history.saveJournal(JOURNAL, List.of(snapshot));
                release.countDown();

                assertThat(delayed.get(20, TimeUnit.SECONDS)).isEqualTo(processed);
                assertThat(history.findByDate(DATE)).containsExactly(snapshot);
                assertThat(history.findPending()).isEmpty();
            } finally {
                release.countDown();
            }
        }
    }

    private static void givenTwoValidSellers() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close(WORKER_B, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));
    }
}
