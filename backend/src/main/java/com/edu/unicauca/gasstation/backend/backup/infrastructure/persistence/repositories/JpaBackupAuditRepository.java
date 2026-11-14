package com.edu.unicauca.gasstation.backend.backup.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.backup.infrastructure.persistence.entities.BackupAuditEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for the backup audit.
 *
 * @author javiersolanop777
 */
public interface JpaBackupAuditRepository extends JpaRepository<BackupAuditEntity, Long> {

    List<BackupAuditEntity> findAllByOrderByStartedAtDesc(Pageable prmPageable);
}
