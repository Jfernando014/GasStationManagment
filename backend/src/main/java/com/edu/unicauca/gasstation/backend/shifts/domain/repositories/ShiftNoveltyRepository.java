package com.edu.unicauca.gasstation.backend.shifts.domain.repositories;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and reading the novelties of the day staff. Implemented by
 * {@code infrastructure.persistence.adapters.ShiftNoveltyRepositoryImpl}. Novelties are always returned with their
 * shift codes and roles loaded.
 */
public interface ShiftNoveltyRepository {

    /** Creates the novelty, or updates it when it already exists (cancelling it is saving it cancelled). */
    ShiftNovelty save(ShiftNovelty novelty);

    /** A novelty by id, active or cancelled. */
    Optional<ShiftNovelty> findById(UUID id);

    /**
     * Active (not cancelled) novelties between both dates (inclusive), in a single query, in registration order
     * (creation time, then id).
     */
    List<ShiftNovelty> findByWorkDateBetween(LocalDate from, LocalDate to);
}
