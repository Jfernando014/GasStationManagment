package com.edu.unicauca.gasstation.backend.sales.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {
    Optional<Journal> findByDate(LocalDate date);
    
    @Query("SELECT MAX(j.consecutive) FROM Journal j")
    Optional<Long> findMaxConsecutive();
}
