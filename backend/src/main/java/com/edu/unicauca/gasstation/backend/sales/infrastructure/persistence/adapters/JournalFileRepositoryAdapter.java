package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalFileRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.JournalFileEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.JournalFileJpaRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JournalFileRepositoryAdapter implements JournalFileRepository {

    private final JournalFileJpaRepository fileJpaRepository;
    private final JournalRepository journalRepository;

    public JournalFileRepositoryAdapter(
            JournalFileJpaRepository fileJpaRepository,
            JournalRepository journalRepository) {
        this.fileJpaRepository = fileJpaRepository;
        this.journalRepository = journalRepository;
    }

    @Override
    @Transactional
    public JournalFile save(JournalFile file) {
        JournalFileEntity entity = fileJpaRepository
                .findByJournal_IdAndFileName(file.journalId(), file.fileName())
                .orElseGet(() -> {
                    JournalFileEntity created = new JournalFileEntity();
                    created.setJournal(journalRepository.getReferenceById(file.journalId()));
                    created.setFileName(file.fileName());
                    return created;
                });
        entity.setContentType(file.contentType());
        entity.setContent(file.content());
        return toDomain(fileJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalFile> findAllByJournalId(Long journalId) {
        return fileJpaRepository.findAllByJournal_IdOrderByUploadedAtAscFileNameAsc(journalId).stream()
                .map(JournalFileRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalFile> findByIdAndJournalId(Long fileId, Long journalId) {
        return fileJpaRepository.findByIdAndJournal_Id(fileId, journalId)
                .map(JournalFileRepositoryAdapter::toDomain);
    }

    private static JournalFile toDomain(JournalFileEntity entity) {
        return new JournalFile(entity.getId(), entity.getJournal().getId(), entity.getFileName(),
                entity.getContentType(), entity.getContent(), entity.getUploadedAt());
    }
}