package com.edu.unicauca.gasstation.backend.backup.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAudit;
import com.edu.unicauca.gasstation.backend.backup.domain.repositories.BackupAuditRepository;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.persistence.entities.BackupAuditEntity;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.persistence.repositories.JpaBackupAuditRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * Stores and reads the backup audit.
 *
 * @author javiersolanop777
 */
@Repository
public class BackupAuditRepositoryAdapter implements BackupAuditRepository {

    private final JpaBackupAuditRepository atrRepository;

    public BackupAuditRepositoryAdapter(JpaBackupAuditRepository prmRepository) {
        this.atrRepository = prmRepository;
    }

    @Override
    public BackupAudit save(BackupAudit prmAudit) {
        BackupAuditEntity objEntity = toEntity(prmAudit);
        return toDomain(atrRepository.save(objEntity));
    }

    @Override
    public List<BackupAudit> findRecent(int prmLimit) {
        int varLimit = Math.max(1, prmLimit);
        return atrRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, varLimit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private BackupAuditEntity toEntity(BackupAudit prmAudit) {
        BackupAuditEntity objEntity = new BackupAuditEntity();
        objEntity.setId(prmAudit.id());
        objEntity.setUsername(prmAudit.username());
        objEntity.setAction(prmAudit.action());
        objEntity.setFileName(prmAudit.fileName());
        objEntity.setSizeBytes(prmAudit.sizeBytes());
        objEntity.setStatus(prmAudit.status());
        objEntity.setMessage(prmAudit.message());
        objEntity.setClientIp(prmAudit.clientIp());
        objEntity.setStartedAt(prmAudit.startedAt());
        objEntity.setFinishedAt(prmAudit.finishedAt());
        return objEntity;
    }

    private BackupAudit toDomain(BackupAuditEntity prmEntity) {
        return new BackupAudit(prmEntity.getId(), prmEntity.getUsername(), prmEntity.getAction(),
                prmEntity.getFileName(), prmEntity.getSizeBytes(), prmEntity.getStatus(),
                prmEntity.getMessage(), prmEntity.getClientIp(), prmEntity.getStartedAt(),
                prmEntity.getFinishedAt());
    }
}
