package com.edu.unicauca.gasstation.backend.workers.domain.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Storage of the processing state of each date. */
public interface HoursSyncRepository {

    /** Creates the state of the date or replaces it. */
    HoursSync save(HoursSync sync);

    Optional<HoursSync> findByDate(LocalDate workDate);

    /** States of every date from {@code from} to {@code to}, both included, ordered by date. */
    List<HoursSync> findBetween(LocalDate from, LocalDate to);

    /** The latest date that has a state, whatever it is; empty if none has. */
    Optional<LocalDate> findLastDate();

    /** Dates still pending, oldest first. */
    List<HoursSync> findPending();

    /** Dates still pending from {@code from} to {@code to}, both included, oldest first. */
    List<LocalDate> findPendingDatesBetween(LocalDate from, LocalDate to);
}
