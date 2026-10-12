package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import java.util.List;
import java.util.Optional;

public interface JournalFileRepository {

    JournalFile save(JournalFile file);

    List<JournalFile> findAllByJournalId(Long journalId);

    Optional<JournalFile> findByIdAndJournalId(Long fileId, Long journalId);
}