package com.edu.unicauca.gasstation.backend.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceQueryService;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.AdministrativeExpenseServiceConfiguration;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.AdministrativeExpenseRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.CashMovementRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.JournalReadAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SavingsFundMovementRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.services.CashServiceImpl;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.services.SavingsFundServiceImpl;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves with a real PostgreSQL (schema created by Flyway) that CashBalanceQuery is wired by Spring and builds the
 * table from the real journal, savings fund, expense and cash tables, through the public contract only.
 */
@SpringBootTest(classes = CashBalanceQueryIntegrationTest.Application.class, properties = {
        "server.port=0", "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false",
        "springdoc.swagger-ui.path=/swagger-ui.html", "springdoc.api-docs.path=/v3/api-docs"})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CashBalanceQueryIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.edu.unicauca.gasstation.backend")
    @EnableJpaRepositories("com.edu.unicauca.gasstation.backend")
    @Import({CashBalanceQueryService.class, JournalReadAdapter.class,
            SavingsFundMovementRepositoryAdapter.class, SavingsFundServiceImpl.class,
            AdministrativeExpenseRepositoryAdapter.class, AdministrativeExpenseServiceConfiguration.class,
            CashMovementRepositoryAdapter.class, CashServiceImpl.class})
    static class Application {
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final LocalDate DAY = LocalDate.of(2026, 8, 5);

    @Autowired
    private CashBalanceQuery cashBalanceQuery;
    @Autowired
    private SavingsFundService savingsFund;
    @Autowired
    private CashService cash;
    @Autowired
    private AdministrativeExpenseService expenses;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.execute("TRUNCATE TABLE journals, savings_fund_movement, cash_movement, administrative_expense CASCADE");
    }

    private void insertJournal(LocalDate date, long consecutive, String status) {
        Long id = jdbc.queryForObject(
                "INSERT INTO journals (journal_date, consecutive, status, created_at) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, Date.valueOf(date), consecutive, status, Timestamp.valueOf(date.atTime(7, 0)));
        jdbc.update("INSERT INTO journal_steps (journal_id, step_number, name, status) VALUES (?, 1, 'First', 'COMPLETED')", id);
        jdbc.update("INSERT INTO journal_steps (journal_id, step_number, name, status) VALUES (?, 2, 'Second', 'PENDING')", id);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void theQueryIsWiredAndReturnsEmptyWhenTheDateHasNoJournal() {
        assertThat(cashBalanceQuery.findDailyCashBalance(DAY)).isEmpty();
    }

    @Test
    void buildsTheTableFromTheRealTables() {
        insertJournal(DAY, 288L, "OPEN");
        for (int day = 1; day <= 5; day++) {
            savingsFund.registerContribution(LocalDate.of(2026, 8, day), cop(250_000), null);
        }
        savingsFund.registerPayment(DAY, cop(300_000), "Rent");
        expenses.register(DAY, cop(45_000), "Papelería");
        expenses.register(DAY, cop(30_000), "Vale transporte");
        expenses.register(DAY.plusDays(1), cop(999_999), "Another day");
        cash.register(CashMovementType.INITIAL_BALANCE, LocalDate.of(2026, 8, 1), cop(2_000_000), "Opening");
        cash.register(CashMovementType.DEPOSIT, LocalDate.of(2026, 8, 4), cop(500_000), "Bank deposit");
        cash.register(CashMovementType.ADJUSTMENT, DAY.plusDays(1), cop(100_000), "Later adjustment");

        CashBalanceTableInfo table = cashBalanceQuery.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.date()).isEqualTo(DAY);
        assertThat(table.journalConsecutive()).isEqualTo(288L);
        assertThat(table.journalStatus()).isEqualTo("OPEN");
        assertThat(table.savingsFund().previousBalance()).isEqualByComparingTo("1000000");
        assertThat(table.savingsFund().contribution()).isEqualByComparingTo("250000");
        assertThat(table.savingsFund().payments()).isEqualByComparingTo("300000");
        assertThat(table.savingsFund().accumulatedBalance()).isEqualByComparingTo("950000");
        assertThat(table.expenses().items()).extracting(CashBalanceTableInfo.ExpenseItemInfo::description)
                .containsExactlyInAnyOrder("Papelería", "Vale transporte");
        assertThat(table.expenses().total()).isEqualByComparingTo("75000");
        assertThat(table.cashBalance()).isEqualByComparingTo("1500000");
        assertThat(table.dailyCash()).isNull();
        assertThat(table.dominusAccumulatedBalance()).isNull();
        assertThat(table.difference()).isNull();
    }

    @Test
    void aDayWithoutMovementsStillGivesTheTableWithZeroTotals() {
        insertJournal(DAY, 289L, "CLOSED");

        CashBalanceTableInfo table = cashBalanceQuery.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.journalStatus()).isEqualTo("CLOSED");
        assertThat(table.expenses().items()).isEmpty();
        assertThat(table.expenses().total()).isEqualByComparingTo("0");
        assertThat(table.savingsFund().accumulatedBalance()).isEqualByComparingTo("0");
        assertThat(table.cashBalance()).isEqualByComparingTo("0");
    }
}
