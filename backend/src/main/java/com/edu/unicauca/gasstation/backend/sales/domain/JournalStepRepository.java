package com.edu.unicauca.gasstation.backend.sales.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalStepRepository extends JpaRepository<JournalStep, Long> {
    Optional<JournalStep> findByJournalIdAndStepNumber(Long journalId, Integer stepNumber);
}
