package com.edu.unicauca.gasstation.backend.competition.domain.services;

import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.*;
import com.edu.unicauca.gasstation.backend.competition.exception.*;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionService {
    private static final Instant EARLIEST = Instant.parse("0001-01-01T00:00:00Z");
    private final CompetitorRepository competitors;
    private final ObservationRepository observations;

    @Transactional
    public Competitor register(String name, String brand, String location) {
        return competitors.save(new Competitor(null, name, brand, location, true));
    }
    public Competitor find(long id) {
        validId(id);
        return competitors.findById(id).orElseThrow(() -> new CompetitorNotFoundException(id));
    }
    public List<Competitor> list(Boolean active) { return competitors.findAll(active); }

    @Transactional
    public Competitor update(long id, String name, String brand, String location) {
        var current = locked(id);
        return competitors.save(new Competitor(id, name, brand, location, current.active()));
    }
    @Transactional
    public Competitor changeStatus(long id, boolean active) {
        var current = locked(id);
        return competitors.save(new Competitor(id, current.name(), current.brand(), current.location(), active));
    }
    private Competitor locked(long id) {
        validId(id);
        return competitors.findByIdForUpdate(id).orElseThrow(() -> new CompetitorNotFoundException(id));
    }

    @Transactional
    public PriceObservation observe(long competitorId, FuelType fuelType, BigDecimal price, Instant observedAt) {
        if (observedAt == null) throw new IllegalArgumentException("observedAt is required");
        var competitor = locked(competitorId);
        if (!competitor.active()) throw new InactiveCompetitorException(competitorId);
        var timestamp = cutoff(observedAt);
        return observations.append(new PriceObservation(null, competitorId, fuelType, price, timestamp, null));
    }
    public PriceObservation observation(long id) {
        validId(id);
        return observations.findById(id).orElseThrow(() -> new ObservationNotFoundException(id));
    }
    public ObservationRepository.History history(Long competitorId, FuelType fuelType, Instant from, Instant to, int page, int size) {
        if (competitorId != null) find(competitorId);
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("page >= 0 and size 1..200 required");
        var end = cutoff(to);
        var start = from == null ? EARLIEST : cutoff(from);
        if (start.isAfter(end)) throw new IllegalArgumentException("from must not be after to");
        return observations.findHistory(competitorId, fuelType, start, end, page, size);
    }
    public List<PriceObservation> latest(Long competitorId, FuelType fuelType, Instant asOf, boolean includeInactive) {
        if (competitorId != null) find(competitorId);
        return observations.findLatest(competitorId, fuelType, cutoff(asOf), includeInactive);
    }
    public static Instant cutoff(Instant requested) {
        var now = Instant.now();
        var result = requested == null ? now : requested;
        if (result.isBefore(EARLIEST) || result.isAfter(now)) throw new IllegalArgumentException("Date must be between year 1 and now");
        return result.truncatedTo(ChronoUnit.MICROS);
    }
    private static void validId(long id) { if (id <= 0) throw new IllegalArgumentException("ID must be positive"); }
}
