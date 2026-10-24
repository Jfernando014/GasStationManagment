package com.edu.unicauca.gasstation.backend.competition.domain.services;

import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.competition.domain.models.PriceCalculations.*;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.ObservationRepository;
import com.edu.unicauca.gasstation.backend.inventory.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketPriceService {
    private static final ZoneId STATION_ZONE = ZoneId.of("America/Bogota");
    private final ObservationRepository observations;
    private final FuelPriceQuery bellavista;

    public record FuelComparison(FuelType fuelType, BigDecimal bellavistaPrice, LocalDate bellavistaValidFrom,
            Summary competition, Difference versusAverage, Difference versusMinimum, List<PriceObservation> observations) { }
    public record Comparison(Instant asOf, LocalDate bellavistaDate, String mode, String unit,
            boolean activeCompetitorsOnly, List<FuelComparison> fuels) { }
    public record Simulation(Instant asOf, FuelType fuelType, String unit, BigDecimal proposedPrice,
            BigDecimal acquisitionCost, Summary competition, Difference versusAverage, Difference versusMinimum,
            Margin estimatedMargin, List<PriceObservation> observations) { }

    public Comparison compare(FuelType fuelType, Instant requested) {
        var asOf = CompetitionService.cutoff(requested);
        var date = asOf.atZone(STATION_ZONE).toLocalDate();
        var latest = observations.findLatest(null, fuelType, asOf, false);
        var types = fuelType == null ? List.of(FuelType.values()) : List.of(fuelType);
        var fuels = types.stream().map(type -> {
            var selected = latest.stream().filter(o -> o.fuelType() == type).toList();
            var summary = PriceCalculations.summarize(selected);
            var own = bellavista.findEffectivePrice(type, date);
            var price = own.map(FuelPriceInfo::getPricePerGallon).orElse(null);
            return new FuelComparison(type, price, own.map(FuelPriceInfo::getValidFrom).orElse(null), summary,
                    PriceCalculations.difference(price, summary.average()), PriceCalculations.difference(price, summary.minimum()), selected);
        }).toList();
        return new Comparison(asOf, date, requested == null ? "CURRENT_LATEST" : "AS_OF", "COP_PER_GALLON", true, fuels);
    }

    public Simulation simulate(FuelType fuelType, BigDecimal proposedPrice, BigDecimal acquisitionCost, Instant requested) {
        if (fuelType == null) throw new IllegalArgumentException("fuelType is required");
        PriceCalculations.money(proposedPrice, false);
        var margin = PriceCalculations.margin(proposedPrice, acquisitionCost);
        var asOf = CompetitionService.cutoff(requested);
        var latest = observations.findLatest(null, fuelType, asOf, false);
        var summary = PriceCalculations.summarize(latest);
        return new Simulation(asOf, fuelType, "COP_PER_GALLON", proposedPrice, acquisitionCost, summary,
                PriceCalculations.difference(proposedPrice, summary.average()), PriceCalculations.difference(proposedPrice, summary.minimum()), margin, latest);
    }
}
