package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.JournalFileEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalFileJpaRepository extends JpaRepository<JournalFileEntity, Long> {

    List<JournalFileEntity> findAllByJournal_IdOrderByUploadedAtAscFileNameAsc(Long journalId);

    Optional<JournalFileEntity> findByIdAndJournal_Id(Long fileId, Long journalId);

    Optional<JournalFileEntity> findByJournal_IdAndFileName(Long journalId, String fileName);
}