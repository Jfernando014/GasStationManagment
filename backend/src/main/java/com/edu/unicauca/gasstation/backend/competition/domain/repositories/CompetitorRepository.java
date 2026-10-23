package com.edu.unicauca.gasstation.backend.competition.domain.repositories;

import com.edu.unicauca.gasstation.backend.competition.domain.models.Competitor;
import java.util.List;
import java.util.Optional;

public interface CompetitorRepository {
    Competitor save(Competitor competitor);
    Optional<Competitor> findById(long id);
    List<Competitor> findAll(Boolean active);
    // Serializes status changes with observation creation, without exposing internal JPA entities.
    Optional<Competitor> findByIdForUpdate(long id);
}
