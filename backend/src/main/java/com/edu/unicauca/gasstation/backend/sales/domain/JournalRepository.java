package com.edu.unicauca.gasstation.backend.sales.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {
    Optional<Journal> findByDate(LocalDate date);

    List<Journal> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT MAX(j.consecutive) FROM Journal j")
    Optional<Long> findMaxConsecutive();

    @Query("SELECT MAX(j.date) FROM Journal j WHERE j.status = com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus.CLOSED")
    Optional<LocalDate> findLatestClosedDate();
}
