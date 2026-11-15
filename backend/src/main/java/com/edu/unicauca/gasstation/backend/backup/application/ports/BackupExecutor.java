package com.edu.unicauca.gasstation.backend.backup.application.ports;

import java.nio.file.Path;

/**
 * Runs the PostgreSQL dump and restore tools.
 *
 * @author javiersolanop777
 */
public interface BackupExecutor {

    void dump(Path prmTarget);

    void restore(Path prmSource);
}
