package com.edu.unicauca.gasstation.backend.backup.application.ports;

/**
 * Restarts the application once a restore has finished.
 *
 * @author javiersolanop777
 */
public interface BackupRestarter {

    void restartAfterRestore();
}
