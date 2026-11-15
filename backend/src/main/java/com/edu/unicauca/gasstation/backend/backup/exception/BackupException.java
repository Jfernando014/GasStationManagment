package com.edu.unicauca.gasstation.backend.backup.exception;

/**
 * Thrown when a backup or restore operation cannot continue.
 *
 * @author javiersolanop777
 */
public class BackupException extends RuntimeException {

    /** Why the operation failed. */
    public enum Reason {
        BUSY,
        RESTORE_DISABLED,
        CONFIRMATION_REQUIRED,
        INVALID_FILE,
        FILE_TOO_LARGE,
        DUMP_FAILED,
        RESTORE_FAILED,
        MIGRATION_FAILED,
        IO_ERROR
    }

    private final Reason atrReason;

    public BackupException(Reason prmReason, String prmMessage) {
        super(prmMessage);
        this.atrReason = prmReason;
    }

    public BackupException(Reason prmReason, String prmMessage, Throwable prmCause) {
        super(prmMessage, prmCause);
        this.atrReason = prmReason;
    }

    public Reason getReason() {
        return atrReason;
    }
}
