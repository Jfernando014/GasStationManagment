package com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/** Flyway validation and upgrade against a disposable PostgreSQL, never the development database. */
@Testcontainers
class ShiftSaleHistoryMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migratesAnEmptyDatabaseAndValidatesWithoutReapplyingAnything() {
        Flyway flyway = flyway("empty_statistics", null);
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(33);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("34");
        assertThat(flyway.info().applied()).noneMatch(m -> m.getVersion() != null && "31".equals(m.getVersion().getVersion()));
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
    }

    @Test
    void upgradesVersion32WithoutChangingSourceDataOrExistingMigrationChecksums() throws Exception {
        Flyway baseline = flyway("upgrade_statistics", "32");
        assertThat(baseline.migrate().migrationsExecuted).isEqualTo(31);
        Map<String, Integer> oldChecksums = new LinkedHashMap<>();
        Arrays.stream(baseline.info().applied()).filter(m -> m.getVersion() != null)
                .forEach(m -> oldChecksums.put(m.getVersion().getVersion(), m.getChecksum()));
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO upgrade_statistics.vendor_shift_closes
                    (journal_date, vendor_document, vendor_name, shift_name, result_status)
                    VALUES ('2026-11-10', '001001', 'Original seller', 'DIA', 'CUADRA')
                    """);
            statement.execute("""
                    INSERT INTO upgrade_statistics.journals (journal_date, consecutive, status, created_at)
                    VALUES ('2026-11-10', 105, 'CLOSED', CURRENT_TIMESTAMP)
                    """);
        }

        Flyway upgrade = flyway("upgrade_statistics", null);
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(2);
        Arrays.stream(upgrade.info().applied()).filter(m -> m.getVersion() != null)
                .filter(m -> oldChecksums.containsKey(m.getVersion().getVersion()))
                .forEach(m -> assertThat(m.getChecksum()).isEqualTo(oldChecksums.get(m.getVersion().getVersion())));
        upgrade.validate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("""
                    SELECT vendor_document, vendor_name, shift_name FROM upgrade_statistics.vendor_shift_closes
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("vendor_document")).isEqualTo("001001");
                assertThat(rows.getString("vendor_name")).isEqualTo("Original seller");
                assertThat(rows.getString("shift_name")).isEqualTo("DIA");
                assertThat(rows.next()).isFalse();
            }
            try (var rows = statement.executeQuery("SELECT consecutive, status FROM upgrade_statistics.journals")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("consecutive")).isEqualTo(105);
                assertThat(rows.getString("status")).isEqualTo("CLOSED");
            }
            try (var rows = statement.executeQuery("""
                    SELECT character_maximum_length FROM information_schema.columns
                    WHERE table_schema = 'upgrade_statistics' AND table_name = 'vendor_shift_closes'
                        AND column_name = 'vendor_document'
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(30);
            }
            try (var rows = statement.executeQuery("SELECT count(*) FROM upgrade_statistics.statistics_journal_sales")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isZero();
            }
            try (var rows = statement.executeQuery("SELECT count(*) FROM upgrade_statistics.statistics_shift_sale")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isZero();
            }
        }
    }

    private static Flyway flyway(String schema, String version) {
        var configuration = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).locations("classpath:db/migration");
        if (version != null) {
            configuration.target(version);
        }
        return configuration.load();
    }
}
