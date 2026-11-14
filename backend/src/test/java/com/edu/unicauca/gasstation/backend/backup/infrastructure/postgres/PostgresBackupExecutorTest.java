package com.edu.unicauca.gasstation.backend.backup.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.backup.infrastructure.config.BackupProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the pg_dump and pg_restore command construction without running the tools.
 *
 * @author javiersolanop777
 */
class PostgresBackupExecutorTest {

    @TempDir
    Path atrTempDir;

    private PostgresBackupExecutor atrExecutor;

    @BeforeEach
    void setUp() {
        BackupProperties objProperties = new BackupProperties();
        objProperties.setDir(atrTempDir.toString());
        atrExecutor = new PostgresBackupExecutor(objProperties,
                "jdbc:postgresql://localhost:5432/gasstation_db", "postgres_user", "secret");
    }

    @Test
    void dumpCommandExcludesTheAuditTableAndItsSequence() {
        List<String> objCommand = atrExecutor.buildDumpCommand(atrTempDir.resolve("backup.dump"));

        assertThat(objCommand).contains("--exclude-table=backup_audit",
                "--exclude-table=backup_audit_id_seq");
    }

    @Test
    void restoreCommandUsesTheFilteredList() {
        Path objSource = atrTempDir.resolve("backup.dump");
        Path objList = atrTempDir.resolve("backup.list");

        List<String> objCommand = atrExecutor.buildRestoreCommand(objSource, objList);

        assertThat(objCommand).contains("--use-list=" + objList.toAbsolutePath());
        assertThat(objCommand).contains(objSource.toAbsolutePath().toString());
    }

    @Test
    void filterAuditTocRemovesTheAuditObjectsAndKeepsTheRest() {
        String objToc = """
                ;
                ; Archive created at 2026-11-14 16:22:24
                ;
                100; 1259 17165 SEQUENCE public backup_audit_id_seq postgres_user
                101; 0 0 SEQUENCE SET public backup_audit_id_seq postgres_user
                102; 1259 17164 TABLE public backup_audit postgres_user
                103; 1259 16616 TABLE public cash_movement postgres_user
                104; 2606 16620 FK CONSTRAINT public cash_movement cash_movement_backup_audit_id_fkey postgres_user
                """;

        String objFiltered = PostgresBackupExecutor.filterAuditToc(objToc);

        assertThat(objFiltered)
                .doesNotContain("SEQUENCE public backup_audit_id_seq")
                .doesNotContain("SEQUENCE SET public backup_audit_id_seq")
                .doesNotContain("TABLE public backup_audit ")
                .contains("TABLE public cash_movement")
                .contains("cash_movement_backup_audit_id_fkey")
                .contains("; Archive created at 2026-11-14 16:22:24");
    }
}
