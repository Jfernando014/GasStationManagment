package com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.*;
import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities.*;
import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.repositories.*;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionRepositoryAdapter implements CompetitorRepository {
    private final JpaCompetitorRepository competitors;

    @Override @Transactional
    public Competitor save(Competitor value) {
        var entity = new CompetitorEntity();
        entity.setId(value.id()); entity.setName(value.name()); entity.setBrand(value.brand());
        entity.setLocation(value.location()); entity.setActive(value.active());
        return toDomain(competitors.saveAndFlush(entity));
    }
    @Override public Optional<Competitor> findById(long id) { return competitors.findById(id).map(CompetitionRepositoryAdapter::toDomain); }
    @Override public Optional<Competitor> findByIdForUpdate(long id) { return competitors.findLocked(id).map(CompetitionRepositoryAdapter::toDomain); }
    @Override public List<Competitor> findAll(Boolean active) { return competitors.findFiltered(active).stream().map(CompetitionRepositoryAdapter::toDomain).toList(); }
    private static Competitor toDomain(CompetitorEntity c) { return new Competitor(c.getId(), c.getName(), c.getBrand(), c.getLocation(), c.isActive()); }
}
