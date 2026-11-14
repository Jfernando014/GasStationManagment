package com.edu.unicauca.gasstation.backend.backup.domain.repositories;

import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAudit;
import java.util.List;

/**
 * Stores the backup audit.
 *
 * @author javiersolanop777
 */
public interface BackupAuditRepository {

    BackupAudit save(BackupAudit prmAudit);

    List<BackupAudit> findRecent(int prmLimit);
}
