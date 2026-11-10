package com.edu.unicauca.gasstation.backend.statistics.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.incentives.domain.services.AdditiveSalesRegistrationService;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.listeners.JournalClosedAdditiveSalesListener;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryAlertService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.listeners.JournalClosingInventoryListener;
import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.statistics.application.ShiftSaleHistoryRecoveryService;
import com.edu.unicauca.gasstation.backend.statistics.application.ShiftSaleHistoryRegistrationService;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.infrastructure.config.ShiftSaleHistoryRecoveryConfiguration;
import com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.adapters.ShiftSaleHistoryRepositoryAdapter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/** Real journal closing, JPA transaction manager, Modulith durable registry and PostgreSQL; only sources are mocked. */
@Testcontainers
class ShiftSaleHistoryEventIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transactions;
    private static JournalService journals;
    private static ShiftSaleHistoryRepository history;
    private static ShiftSaleHistoryRecoveryService recovery;
    private static SellerCloseQuery sales;
    private static DayStaffQuery staff;
    private static AdditiveSalesRegistrationService additives;
    private static InventoryAlertService inventory;
    private static TankMeasurementRepository measurements;
    private static boolean schedulerEnabled;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan({"com.edu.unicauca.gasstation.backend.sales.domain", "org.springframework.modulith.events.jpa"})
    @EnableJpaRepositories(basePackageClasses = JournalRepository.class)
    @Import({JournalService.class, JournalClosedShiftSalesListener.class, ShiftSaleHistoryRegistrationService.class,
            ShiftSaleHistoryRepositoryAdapter.class, ShiftSaleHistoryRecoveryService.class,
            ShiftSaleHistoryRecoveryConfiguration.class, JournalClosedAdditiveSalesListener.class,
            JournalClosingInventoryListener.class})
    static class EventApplication {
    }

    @BeforeAll
    static void startIsolatedApplication() {
        restartContext(false);
    }

    private static void restartContext(boolean schedule) {
        if (context != null) {
            context.close();
        }
        sales = mock(SellerCloseQuery.class);
        staff = mock(DayStaffQuery.class);
        additives = mock(AdditiveSalesRegistrationService.class);
        inventory = mock(InventoryAlertService.class);
        measurements = mock(TankMeasurementRepository.class);
        givenTwoSellers();
        context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRES.getUsername(),
                "spring.datasource.password=" + POSTGRES.getPassword(),
                "spring.datasource.hikari.maximum-pool-size=12",
                "spring.datasource.hikari.connection-init-sql=SET lock_timeout = '10s'",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.modulith.republish-outstanding-events-on-restart=false",
                "statistics.shift-sales.recovery.enabled=" + schedule,
                "statistics.shift-sales.recovery.delay-ms=100",
                "statistics.shift-sales.recovery.initial-delay-ms=100"
        ).applyTo(context);
        context.registerBean(SellerCloseQuery.class, () -> sales);
        context.registerBean(DayStaffQuery.class, () -> staff);
        context.registerBean(AdditiveSalesRegistrationService.class, () -> additives);
        context.registerBean(InventoryAlertService.class, () -> inventory);
        context.registerBean(TankMeasurementRepository.class, () -> measurements);
        context.register(EventApplication.class);
        context.refresh();
        jdbc = context.getBean(JdbcTemplate.class);
        transactions = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        journals = context.getBean(JournalService.class);
        history = context.getBean(ShiftSaleHistoryRepository.class);
        recovery = context.getBean(ShiftSaleHistoryRecoveryService.class);
        schedulerEnabled = schedule;
    }

    @AfterAll
    static void closeApplication() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void clearOnlyTheContainerAndResetSources() {
        if (schedulerEnabled) {
            restartContext(false);
        }
        jdbc.execute("TRUNCATE statistics_shift_sale, statistics_journal_sales, journal_steps, journals, "
                + "event_publication, vendor_shift_closes CASCADE");
        reset(sales, staff, additives, inventory, measurements);
        givenTwoSellers();
        var measurement = new TankMeasurement();
        measurement.setJournalDate(DATE);
        when(measurements.findLatestBefore(any())).thenReturn(Optional.of(measurement));
    }

    @Test
    void committedClosingRegistersExactTitularAndSupportHistoryAndAllExistingConsumersStillRun() {
        Long id = journals.getOrCreateJournal(DATE).getId();

        transactions.executeWithoutResult(transaction -> {
            journals.closeJournal(id);
            assertThat(history.findProcessing(DATE)).isEmpty();
            verifyNoInteractions(sales, staff, additives, inventory);
        });

        var state = history.findProcessing(DATE).orElseThrow();
        assertThat(state.status()).isEqualTo(PROCESSED);
        assertThat(state.journal().journalId()).isEqualTo(id);
        assertThat(state.journal().journalDate()).isEqualTo(DATE);
        assertThat(state.saleCount()).isEqualTo(2);
        var rows = history.findByDate(DATE);
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().sales()).isEqualTo(AMOUNTS);
        assertThat(rows.getFirst().support()).isFalse();
        assertThat(rows.getLast().support()).isTrue();
        assertThat(rows.getLast().workerId()).isEqualTo(WORKER_B);
        assertThat(rows.getLast().workedCode()).isEqualTo("12-7");
        assertThat(sourceStatus()).isEqualTo("CLOSED");
        assertThat(incompleteOwnPublications()).isZero();
        assertThat(ownPublications()).isEqualTo(1);
        verify(additives).registerClosedJournal(any());
        verify(inventory).reconcile(DATE);
    }

    @Test
    void rolledBackClosingCreatesNeitherHistoryNorDurablePublications() {
        Long id = journals.getOrCreateJournal(DATE).getId();

        assertThatThrownBy(() -> transactions.executeWithoutResult(transaction -> {
            journals.closeJournal(id);
            throw new IllegalStateException("rollback closing");
        })).hasMessage("rollback closing");

        assertThat(sourceStatus()).isEqualTo("OPEN");
        assertThat(history.findProcessing(DATE)).isEmpty();
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_publication", Integer.class)).isZero();
        verifyNoInteractions(sales, staff, additives, inventory);
    }

    @Test
    void repeatedEventDoesNotReadSourcesOrOverwriteConfirmedSnapshots() {
        closeJournal();
        var state = history.findProcessing(DATE).orElseThrow();
        var rows = history.findByDate(DATE);
        reset(sales, staff);

        publish(event(state.journal()));
        recovery.recoverPending();

        assertThat(history.findProcessing(DATE).orElseThrow()).isEqualTo(state);
        assertThat(history.findByDate(DATE)).isEqualTo(rows);
        assertThat(incompleteOwnPublications()).isZero();
        verifyNoInteractions(sales, staff);
    }

    @Test
    void concurrentCommittedDuplicateEventsKeepOneAtomicConfirmedDay() throws Exception {
        var barrier = new CyclicBarrier(2);
        when(sales.findVendorClosesByDate(DATE)).thenAnswer(invocation -> {
            barrier.await(10, TimeUnit.SECONDS);
            return List.of(close(WORKER_A, "DIA"), close(WORKER_B, "12-7"));
        });
        var event = event(JOURNAL);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> publish(event));
            var second = executor.submit(() -> publish(event));
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        }
        recovery.recoverPending();

        assertThat(history.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(1);
        assertThat(history.findByDate(DATE)).hasSize(2);
        assertThat(history.findPending()).isEmpty();
        assertThat(incompleteOwnPublications()).isZero();
    }

    @Test
    void aClosedJournalWithoutSalesIsConfirmedWithoutInventedRowsOrStaffQueries() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of());

        closeJournal();

        var state = history.findProcessing(DATE).orElseThrow();
        assertThat(state.status()).isEqualTo(PROCESSED);
        assertThat(state.saleCount()).isZero();
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(incompleteOwnPublications()).isZero();
        verifyNoInteractions(staff);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sales", "staff"})
    void aSourceFailureLeavesTheJournalClosedAndDurablePendingWorkThatExplicitRecoveryCompletes(String source) {
        if ("sales".equals(source)) {
            when(sales.findVendorClosesByDate(DATE)).thenThrow(new IllegalStateException("sales offline"));
        } else {
            when(staff.getDayStaff(DATE)).thenThrow(new IllegalStateException("staff offline"));
        }

        closeJournal();

        assertThat(sourceStatus()).isEqualTo("CLOSED");
        var pending = history.findProcessing(DATE).orElseThrow();
        assertThat(pending.status()).isEqualTo(PENDING);
        assertThat(pending.attempts()).isEqualTo(1);
        assertThat(pending.lastError()).contains(source + " offline");
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(incompleteOwnPublications()).isEqualTo(1);
        verify(additives).registerClosedJournal(any());
        verify(inventory).reconcile(DATE);
        reset(sales, staff);
        givenTwoSellers();

        recovery.recoverPending();

        assertThat(history.findProcessing(DATE).orElseThrow().status()).isEqualTo(PROCESSED);
        assertThat(history.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(2);
        assertThat(history.findByDate(DATE)).hasSize(2);
        assertThat(incompleteOwnPublications()).isZero();
    }

    @Test
    void persistenceFailureRollsBackAllSalesAndRemainsRecoverable() {
        var impossible = new ShiftSaleAmounts(new BigDecimal("1E-16384"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"),
                close(DATE, WORKER_B.toString(), "12-7", "Seller B", 3, impossible)));

        closeJournal();

        assertThat(sourceStatus()).isEqualTo("CLOSED");
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(history.findProcessing(DATE).orElseThrow().status()).isEqualTo(PENDING);
        assertThat(history.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(1);
        assertThat(incompleteOwnPublications()).isEqualTo(1);
        givenTwoSellers();

        recovery.recoverPending();

        assertThat(history.findByDate(DATE)).hasSize(2);
        assertThat(history.findProcessing(DATE).orElseThrow().attempts()).isEqualTo(2);
        assertThat(incompleteOwnPublications()).isZero();
    }

    @Test
    void unresolvedIdentityKeepsTheWholeDayPendingAndCountsOneAttemptPerRecoveryPass() {
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));
        closeJournal();

        recovery.recoverPending();

        var pending = history.findProcessing(DATE).orElseThrow();
        assertThat(pending.status()).isEqualTo(PENDING);
        assertThat(pending.attempts()).isEqualTo(2);
        assertThat(pending.lastError()).contains("WORKER_NOT_IN_DAY_STAFF");
        assertThat(history.findByDate(DATE)).isEmpty();
        assertThat(incompleteOwnPublications()).isEqualTo(1);
        givenTwoSellers();
        recovery.recoverPending();
        assertThat(history.findByDate(DATE)).hasSize(2);
        assertThat(incompleteOwnPublications()).isZero();
    }

    @Test
    void durablePublicationSurvivesRestartEvenWhenWritingThePendingMarkerAlsoFailed() {
        jdbc.execute("CREATE FUNCTION statistics_test_reject_marker() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN RAISE EXCEPTION 'simulated statistics storage outage'; END; $$");
        jdbc.execute("CREATE TRIGGER statistics_test_reject_marker BEFORE INSERT ON statistics_journal_sales "
                + "FOR EACH ROW EXECUTE FUNCTION statistics_test_reject_marker()");
        try {
            when(sales.findVendorClosesByDate(DATE)).thenThrow(new IllegalStateException("source also unavailable"));
            closeJournal();
            assertThat(sourceStatus()).isEqualTo("CLOSED");
            assertThat(history.findProcessing(DATE)).isEmpty();
            assertThat(incompleteOwnPublications()).isEqualTo(1);
        } finally {
            jdbc.execute("DROP TRIGGER statistics_test_reject_marker ON statistics_journal_sales");
            jdbc.execute("DROP FUNCTION statistics_test_reject_marker()");
        }

        restartContext(false);
        assertThat(history.findProcessing(DATE)).isEmpty();
        assertThat(incompleteOwnPublications()).isEqualTo(1);
        recovery.recoverPending();

        assertThat(history.findProcessing(DATE).orElseThrow().status()).isEqualTo(PROCESSED);
        assertThat(history.findByDate(DATE)).hasSize(2);
        assertThat(incompleteOwnPublications()).isZero();
    }

    @Test
    void anExplicitScheduledPassRecoversPendingWorkAfterRestartWithoutModulithAutomaticReplay() {
        when(sales.findVendorClosesByDate(DATE)).thenThrow(new IllegalStateException("temporarily offline"));
        closeJournal();
        assertThat(incompleteOwnPublications()).isEqualTo(1);

        restartContext(true);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(history.findProcessing(DATE).orElseThrow().status()).isEqualTo(PROCESSED);
            assertThat(history.findByDate(DATE)).hasSize(2);
            assertThat(incompleteOwnPublications()).isZero();
        });
    }

    @Test
    void statisticsRecoveryDoesNotReplayPendingPublicationsOfExistingConsumers() {
        closeJournal();
        int otherPublications = jdbc.update("UPDATE event_publication SET completion_date = NULL WHERE listener_id <> ?",
                JournalClosedShiftSalesListener.LISTENER_ID);
        assertThat(otherPublications).isEqualTo(2);

        recovery.recoverPending();

        verify(additives, times(1)).registerClosedJournal(any());
        verify(inventory, times(1)).reconcile(DATE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_publication "
                + "WHERE completion_date IS NULL AND listener_id <> ?", Integer.class,
                JournalClosedShiftSalesListener.LISTENER_ID)).isEqualTo(2);
    }

    @Test
    void theUnchangedUpstreamVarchar30StillBlocksARealUuidSellerClose() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO vendor_shift_closes "
                + "(journal_date, vendor_document, vendor_name, shift_name, result_status) VALUES (?, ?, ?, ?, ?)",
                DATE, WORKER_A.toString(), "Seller", "DIA", "CUADRA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(30)");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vendor_shift_closes", Integer.class)).isZero();
    }

    @Test
    void incompletePublicationAfterACommittedSnapshotIsCompletedWithoutRereadingMutableSources() {
        closeJournal();
        var state = history.findProcessing(DATE).orElseThrow();
        var snapshots = history.findByDate(DATE);
        jdbc.update("UPDATE event_publication SET completion_date = NULL WHERE listener_id = ?",
                JournalClosedShiftSalesListener.LISTENER_ID);
        reset(sales, staff);

        recovery.recoverPending();

        assertThat(history.findProcessing(DATE).orElseThrow()).isEqualTo(state);
        assertThat(history.findByDate(DATE)).isEqualTo(snapshots);
        assertThat(incompleteOwnPublications()).isZero();
        verifyNoInteractions(sales, staff);
    }

    private static void givenTwoSellers() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close(WORKER_B, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));
    }

    private static void closeJournal() {
        journals.closeJournal(journals.getOrCreateJournal(DATE).getId());
    }

    private static JournalClosedEvent event(ClosedJournalSnapshot journal) {
        return new JournalClosedEvent(journal.journalId(), journal.journalDate(), journal.consecutive(), journal.closedAt());
    }

    private static void publish(JournalClosedEvent event) {
        transactions.executeWithoutResult(transaction -> context.publishEvent(event));
    }

    private static String sourceStatus() {
        return jdbc.queryForObject("SELECT status FROM journals WHERE journal_date = ?", String.class, DATE);
    }

    private static int ownPublications() {
        return jdbc.queryForObject("SELECT count(*) FROM event_publication WHERE listener_id = ?", Integer.class,
                JournalClosedShiftSalesListener.LISTENER_ID);
    }

    private static int incompleteOwnPublications() {
        return jdbc.queryForObject("SELECT count(*) FROM event_publication WHERE listener_id = ? AND completion_date IS NULL",
                Integer.class, JournalClosedShiftSalesListener.LISTENER_ID);
    }
}