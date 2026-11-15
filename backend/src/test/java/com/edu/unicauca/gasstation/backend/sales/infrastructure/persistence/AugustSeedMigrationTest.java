package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Applies the real August 2026 operational seed to a disposable PostgreSQL database and checks its repeatability.
 *
 * @author javiersolanop777
 */
@Testcontainers
class AugustSeedMigrationTest {

    private static final String SEED = "db/seed/R__seed_august_2026_operational_data.sql";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrate() {
        assertThat(flyway().migrate().migrationsExecuted).isPositive();
        flyway().validate();
    }

    @Test
    void seedsTheAugustOperationalDataAndCanRunAgain() throws SQLException {
        assertSeedContent();

        try (Connection connection = connection()) {
            executeSeed(connection);
            executeSeed(connection);
        }

        assertSeedContent();
    }

    private static void assertSeedContent() throws SQLException {
        try (Connection connection = connection()) {
            assertThat(count(connection, "journals")).isEqualTo(28);
            assertThat(count(connection, "journal_steps")).isEqualTo(28 * 6);
            assertThat(count(connection, "vendor_shift_closes")).isEqualTo(78);
            assertThat(count(connection, "statistics_journal_sales")).isEqualTo(28);
            assertThat(count(connection, "statistics_shift_sale")).isEqualTo(78);
            assertThat(count(connection, "shift_assignment")).isEqualTo(112);
            assertThat(count(connection, "tank_measurement")).isEqualTo(23);
            assertThat(count(connection, "tank_reading")).isEqualTo(138);
            assertThat(longValue(connection, "SELECT COUNT(DISTINCT tank_id) FROM tank_reading")).isEqualTo(6);
            assertThat(count(connection, "fuel_reception")).isEqualTo(11);
            assertThat(count(connection, "administrative_expense")).isEqualTo(12);
            assertThat(count(connection, "savings_fund_movement")).isEqualTo(40);
            assertThat(count(connection, "cash_movement")).isEqualTo(18);

            assertThat(longValue(connection, "SELECT MIN(consecutive) FROM journals")).isEqualTo(289);
            assertThat(longValue(connection, "SELECT MAX(consecutive) FROM journals")).isEqualTo(316);
            assertThat(dateValue(connection, "SELECT MIN(journal_date) FROM tank_measurement")).isEqualTo("2026-07-31");
            assertThat(dateValue(connection, "SELECT MAX(journal_date) FROM tank_measurement")).isEqualTo("2026-08-28");
            assertThat(count(connection, "journals WHERE journal_date = DATE '2026-08-29'")).isZero();

            assertThat(decimalValue(connection, "SELECT SUM(amount) FROM administrative_expense"))
                    .isEqualByComparingTo("187950.00");
            assertThat(decimalValue(connection, "SELECT SUM(points_total) FROM points_colombia_day"))
                    .isEqualByComparingTo("1796955.00");
            assertThat(decimalValue(connection, "SELECT SUM(gallons) FROM fuel_reception"))
                    .isEqualByComparingTo("27440.00");
            assertThat(count(connection, "fuel_reception WHERE reception_date = DATE '2026-08-01'")).isEqualTo(2);

            assertThat(decimalValue(connection, "SELECT SUM(mastercard) FROM credibanco_vendor_day"))
                    .isEqualByComparingTo("66683965.00");
            assertThat(decimalValue(connection, "SELECT SUM(visa) FROM credibanco_vendor_day"))
                    .isEqualByComparingTo("32184828.00");
            assertThat(decimalValue(connection, "SELECT SUM(amex) FROM credibanco_vendor_day"))
                    .isEqualByComparingTo("0.00");
            assertThat(decimalValue(connection, "SELECT SUM(planilla_qr) FROM credibanco_vendor_day"))
                    .isEqualByComparingTo("19978474.00");
            assertThat(decimalValue(connection, "SELECT qr FROM credibanco_previous_month "
                    + "WHERE month_start = DATE '2026-08-01'")).isEqualByComparingTo("21926590.00");
            assertThat(decimalValue(connection, "SELECT planilla_qr FROM credibanco_vendor_day "
                    + "WHERE business_date = DATE '2026-08-27' AND worker_id = "
                    + "(SELECT id FROM worker WHERE document = 'SIN-ASIGNAR-ANA')"))
                    .isEqualByComparingTo("270255.00");

            assertThat(decimalValue(connection,
                    "SELECT SUM(CASE WHEN movement_type = 'CONTRIBUTION' THEN amount ELSE 0 END) "
                            + "FROM savings_fund_movement")).isEqualByComparingTo("9750000.00");
            assertThat(decimalValue(connection,
                    "SELECT SUM(CASE WHEN movement_type = 'PAYMENT' THEN amount ELSE 0 END) "
                            + "FROM savings_fund_movement")).isEqualByComparingTo("7750000.00");
            assertThat(decimalValue(connection, "SELECT amount FROM cash_movement "
                    + "WHERE movement_type = 'DEPOSIT' AND movement_date = DATE '2026-08-02'"))
                    .isEqualByComparingTo("29616000.00");
            assertThat(count(connection, "cash_movement WHERE movement_date = DATE '2026-08-01'")).isZero();

            assertThat(cashDifference(connection, "2026-08-01")).isEqualByComparingTo("-531.00");
            assertThat(cashDifference(connection, "2026-08-02")).isEqualByComparingTo("-964.00");
        }
    }

    private static BigDecimal cashDifference(Connection connection, String date) throws SQLException {
        String sql = """
                SELECT d.accumulated_balance - (
                    (SELECT COALESCE(SUM(CASE
                        WHEN movement_type = 'DEPOSIT' THEN -amount
                        ELSE amount
                    END), 0)
                     FROM cash_movement
                     WHERE movement_date <= CAST(? AS date))
                    +
                    (SELECT COALESCE(SUM(cash), 0)
                     FROM vendor_shift_closes
                     WHERE journal_date <= CAST(? AS date))
                )
                FROM dominus_cash_balance d
                WHERE d.business_date = CAST(? AS date)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) {
                statement.setString(i, date);
            }
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                return rows.getBigDecimal(1);
            }
        }
    }

    private static void executeSeed(Connection connection) {
        ScriptUtils.executeSqlScript(connection,
                new EncodedResource(new ClassPathResource(SEED), StandardCharsets.UTF_8));
    }

    private static int count(Connection connection, String tableOrCondition) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + tableOrCondition)) {
            assertThat(rows.next()).isTrue();
            return rows.getInt(1);
        }
    }

    private static long longValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getLong(1);
        }
    }

    private static String dateValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getDate(1).toLocalDate().toString();
        }
    }

    private static BigDecimal decimalValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getBigDecimal(1);
        }
    }

    private static Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/seed")
                .load();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
