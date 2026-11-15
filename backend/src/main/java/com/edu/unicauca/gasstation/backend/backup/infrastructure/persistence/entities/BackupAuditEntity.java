package com.edu.unicauca.gasstation.backend.backup.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAction;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database row of the backup audit.
 *
 * @author javiersolanop777
 */
@Entity
@Table(name = "backup_audit")
@Getter
@Setter
@NoArgsConstructor
public class BackupAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BackupAction action;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BackupStatus status;

    @Column(length = 500)
    private String message;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
