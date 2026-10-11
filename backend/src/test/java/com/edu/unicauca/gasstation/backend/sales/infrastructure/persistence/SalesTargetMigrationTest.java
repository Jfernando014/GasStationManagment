package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs only against a disposable PostgreSQL container, never the development datasource. */
@Testcontainers
class SalesTargetMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrate() {
        flyway("public").migrate();
    }

    @BeforeEach
    void emptyTargets() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE sales_targets");
        }
    }

    @Test
    void migratesEmptyDatabaseAndIsRepeatableWithoutNewMigrations() {
        Flyway flyway = flyway("public");
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
    }

    @Test
    void upgradesVersionEightWithoutChangingExistingTables() throws SQLException {
        Flyway baseline = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("upgrade_test").locations("classpath:db/migration").target("8").load();
        assertThat(baseline.migrate().migrationsExecuted).isEqualTo(8);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO upgrade_test.users (username, password, role) VALUES ('migration-test', 'test', 'ADMIN')");
        }
        Flyway upgrade = flyway("upgrade_test");
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(2);
        upgrade.validate();
        try (Connection connection = connection(); Statement statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT username FROM upgrade_test.users")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("migration-test");
        }
    }

    @Test
    void generatesIdAndCreationInstantAndPreservesNumericValue() throws SQLException {
        insert(2026, 8, "FUEL_AGGREGATE", "26920.123456789012", "GALLONS");
        try (Connection connection = connection(); Statement statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT id, target_value, created_at FROM sales_targets")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong("id")).isPositive();
            assertThat(rows.getBigDecimal("target_value")).isEqualByComparingTo(new BigDecimal("26920.123456789012"));
            assertThat(rows.getTimestamp("created_at")).isNotNull();
        }
    }

    @Test
    void rejectsAnnualAndMonthlyDuplicates() throws SQLException {
        insert(2026, null, "BASKET", "0", "COP");
        assertSqlState("23505", () -> insert(2026, null, "BASKET", "1", "COP"));
        insert(2026, 8, "BASKET", "8400000", "COP");
        assertSqlState("23505", () -> insert(2026, 8, "BASKET", "1", "COP"));
    }

    @Test
    void permitsIndependentPeriodsAndCategories() throws SQLException {
        insert(2026, null, "FUEL_AGGREGATE", "0", "GALLONS");
        insert(2026, 1, "FUEL_AGGREGATE", "1", "GALLONS");
        insert(2026, 12, "FUEL_AGGREGATE", "1", "GALLONS");
        insert(2027, null, "FUEL_AGGREGATE", "1", "GALLONS");
        insert(2026, null, "BASKET", "1", "COP");
        try (Connection connection = connection(); Statement statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT count(*) FROM sales_targets")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(5);
        }
    }

    @Test
    void rejectsInvalidMonthsAmountsAndCategoryUnitPairs() {
        assertSqlState("23514", () -> insert(2026, 0, "BASKET", "1", "COP"));
        assertSqlState("23514", () -> insert(2026, 13, "BASKET", "1", "COP"));
        assertSqlState("23514", () -> insert(2026, 8, "BASKET", "-1", "COP"));
        assertSqlState("23514", () -> insert(2026, 8, "BASKET", "NaN", "COP"));
        assertSqlState("23514", () -> insert(2026, 8, "FUEL_AGGREGATE", "1", "COP"));
        assertSqlState("23514", () -> insert(2026, 8, "BASKET", "1", "GALLONS"));
        assertSqlState("23514", () -> insert(2026, 8, "MOTOR", "1", "GALLONS"));
        assertSqlState("23514", () -> insert(2026, 8, "BASKET", "1", "UNKNOWN"));
        assertSqlState("22003", () -> insert(2026, 8, "BASKET", "1E18", "COP"));
        assertSqlState("22003", () -> insert(2026, 8, "BASKET", "Infinity", "COP"));
    }

    @Test
    void rejectsNullRequiredFields() {
        assertSqlState("23502", () -> insert(null, null, "BASKET", "1", "COP"));
        assertSqlState("23502", () -> insert(2026, null, null, "1", "COP"));
        assertSqlState("23502", () -> insert(2026, null, "BASKET", null, "COP"));
        assertSqlState("23502", () -> insert(2026, null, "BASKET", "1", null));
        assertSqlState("23502", () -> {
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO sales_targets (target_year, category, target_value, unit, created_at) "
                        + "VALUES (2026, 'BASKET', 1, 'COP', NULL)");
            }
        });
    }

    private static Flyway flyway(String schema) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).locations("classpath:db/migration").load();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void insert(Integer year, Integer month, String category, String value, String unit) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sales_targets (target_year, target_month, category, target_value, unit) VALUES (?, ?, ?, CAST(? AS numeric), ?)")) {
            statement.setObject(1, year);
            statement.setObject(2, month);
            statement.setString(3, category);
            statement.setString(4, value);
            statement.setString(5, unit);
            statement.executeUpdate();
        }
    }

    private static void assertSqlState(String state, SqlAction action) {
        assertThatThrownBy(action::run).isInstanceOf(SQLException.class)
                .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo(state));
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
