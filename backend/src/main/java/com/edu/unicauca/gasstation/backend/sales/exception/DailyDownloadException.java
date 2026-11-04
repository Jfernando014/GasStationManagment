package com.edu.unicauca.gasstation.backend.sales.exception;

/**
 * Thrown when a daily download file cannot be built.
 *
 * @author javiersolanop777
 */
public class DailyDownloadException extends RuntimeException {

    /** Why the download failed. */
    public enum Reason { NO_DATA, GENERATION_FAILED }

    private final Reason atrReason;

    public DailyDownloadException(Reason prmReason, String prmMessage) {
        super(prmMessage);
        this.atrReason = prmReason;
    }

    public DailyDownloadException(Reason prmReason, String prmMessage, Throwable prmCause) {
        super(prmMessage, prmCause);
        this.atrReason = prmReason;
    }

    public Reason getReason() {
        return atrReason;
    }
}
