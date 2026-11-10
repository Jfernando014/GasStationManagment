package com.edu.unicauca.gasstation.backend.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import com.edu.unicauca.gasstation.backend.config.AsyncEventsConfiguration;
import com.edu.unicauca.gasstation.backend.sales.application.SalesParameterService;
import com.edu.unicauca.gasstation.backend.sales.application.SavingsContributionOnJournalClosed;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SalesParameterRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SavingsFundMovementRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.services.SavingsFundServiceImpl;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The daily contribution to the savings fund when a journal is closed, with a real PostgreSQL (Flyway migrations,
 * including the {@code DAILY_SAVINGS} seed of 250.000) and the real Spring Modulith event publication registry.
 */
@SpringBootTest(classes = SavingsContributionOnJournalClosedIntegrationTest.Application.class, properties = {
        "server.port=0", "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false",
        "springdoc.swagger-ui.path=/swagger-ui.html", "springdoc.api-docs.path=/v3/api-docs"})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SavingsContributionOnJournalClosedIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan({"com.edu.unicauca.gasstation.backend", "org.springframework.modulith.events.jpa"})
    @EnableJpaRepositories({"com.edu.unicauca.gasstation.backend", "org.springframework.modulith.events.jpa"})
    @Import({SavingsContributionOnJournalClosed.class, SalesParameterService.class,
            SalesParameterRepositoryAdapter.class, SavingsFundMovementRepositoryAdapter.class,
            SavingsFundServiceImpl.class, AsyncEventsConfiguration.class})
    static class Application {
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String DESCRIPTION = "Aporte diario al cierre de la jornada";

    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private PlatformTransactionManager transactions;
    @Autowired
    private SavingsFundService savingsFund;
    @Autowired
    private SalesParameterService parameters;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.execute("TRUNCATE TABLE savings_fund_movement, event_publication");
        jdbc.update("DELETE FROM sales_parameter WHERE parameter_key = 'DAILY_SAVINGS' AND valid_from > DATE '2026-01-01'");
    }

    private static LocalDate august(int day) {
        return LocalDate.of(2026, 8, day);
    }

    /** Publishes the event inside a transaction, as the code that closes a journal must do. */
    private void close(long journalId, LocalDate date) {
        JournalClosedEvent event = new JournalClosedEvent(journalId, date, 280L + journalId,
                LocalDateTime.of(date, LocalTime.of(20, 0)));
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event));
    }

    private int publications() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM event_publication", Integer.class);
    }

    private int incompletePublications() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL",
                Integer.class);
    }

    private void awaitBalance(LocalDate date, String expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(
                () -> assertThat(savingsFund.getBalanceAt(date)).isEqualByComparingTo(expected));
    }

    private void awaitEveryPublicationCompleted(int expectedPublications) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(publications()).isEqualTo(expectedPublications);
            assertThat(incompletePublications()).isZero();
        });
    }

    @Test
    void closingADayAddsTheDailySavingsToTheFund() {
        close(1L, august(1));

        awaitBalance(august(1), "250000");
        awaitEveryPublicationCompleted(1);
        List<SavingsFundMovement> movements = savingsFund.listMovements();
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getMovementType()).isEqualTo(SavingsFundMovementType.CONTRIBUTION);
        assertThat(movements.get(0).getMovementDate()).isEqualTo(august(1));
        assertThat(movements.get(0).getAmount()).isEqualByComparingTo("250000");
        assertThat(movements.get(0).getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void theFundGrowsBy250000ForEachClosedDay() {
        close(1L, august(1));
        close(2L, august(2));
        close(3L, august(3));

        awaitBalance(august(3), "750000");
        awaitEveryPublicationCompleted(3);
        for (int day = 1; day <= 3; day++) {
            assertThat(savingsFund.summarize(august(day)).contribution()).isEqualByComparingTo("250000");
        }
        assertThat(savingsFund.summarize(august(3)).previousBalance()).isEqualByComparingTo("500000");
    }

    /** A republished event arrives after the first delivery finished, never at the same time. */
    @Test
    void theSameDayDeliveredTwiceIsContributedOnce() {
        close(1L, august(1));
        awaitEveryPublicationCompleted(1);

        close(1L, august(1));

        awaitEveryPublicationCompleted(2);
        assertThat(savingsFund.listMovements()).hasSize(1);
        assertThat(savingsFund.getBalanceAt(august(1))).isEqualByComparingTo("250000");
    }

    @Test
    void aContributionTypedByHandForThatDayIsKept() {
        savingsFund.registerContribution(august(1), new BigDecimal("300000"), "manual");

        close(1L, august(1));

        awaitEveryPublicationCompleted(1);
        assertThat(savingsFund.listMovements()).hasSize(1);
        assertThat(savingsFund.getBalanceAt(august(1))).isEqualByComparingTo("300000");
    }

    @Test
    void aChangeOfTheParameterOnlyAffectsTheDaysFromItsDate() {
        parameters.register(SalesParameterKey.DAILY_SAVINGS, new BigDecimal("300000"), LocalDate.of(2026, 9, 1));

        close(1L, LocalDate.of(2026, 8, 31));
        close(2L, LocalDate.of(2026, 9, 2));

        awaitBalance(LocalDate.of(2026, 9, 2), "550000");
        awaitEveryPublicationCompleted(2);
        assertThat(savingsFund.summarize(LocalDate.of(2026, 8, 31)).contribution()).isEqualByComparingTo("250000");
        assertThat(savingsFund.summarize(LocalDate.of(2026, 9, 2)).contribution()).isEqualByComparingTo("300000");
    }

    @Test
    void ifTheContributionCannotBeRegisteredTheClosingIsNotAffectedAndTheEventStaysPending() {
        LocalDate beforeTheParameter = LocalDate.of(2025, 12, 31);

        assertThatCode(() -> close(9L, beforeTheParameter)).doesNotThrowAnyException();

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(savingsFund.listMovements()).isEmpty());
        assertThat(publications()).isEqualTo(1);
        assertThat(incompletePublications()).isEqualTo(1);
    }
}
