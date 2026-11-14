package com.edu.unicauca.gasstation.backend.backup.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the backup module. Every value can be changed with an environment variable.
 *
 * @author javiersolanop777
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "backup")
public class BackupProperties {

    /** Directory for temporary dump files. */
    private String dir = "/app/backups";

    /** Maximum size accepted for an uploaded dump. */
    private long maxUploadBytes = 512L * 1024L * 1024L;

    /** Maximum time for one pg_dump or pg_restore command. */
    private long timeoutSeconds = 600;

    /** Allows the restore endpoint. */
    private boolean restoreEnabled = true;

    /** Restarts the application after a successful restore. */
    private boolean restartAfterRestore = true;

    /** Path or name of the pg_dump executable. */
    private String dumpCommand = "pg_dump";

    /** Path or name of the pg_restore executable. */
    private String restoreCommand = "pg_restore";

    /** Text the client must send before a restore. */
    private String confirmationPhrase = "RESTAURAR";

    /** Number of audit rows returned to the frontend. */
    private int auditLimit = 20;
}
