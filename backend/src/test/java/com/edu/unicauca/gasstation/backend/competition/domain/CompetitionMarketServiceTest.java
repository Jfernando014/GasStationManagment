package com.edu.unicauca.gasstation.backend.competition.domain;

import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.ObservationRepository;
import com.edu.unicauca.gasstation.backend.competition.domain.services.MarketPriceService;
import com.edu.unicauca.gasstation.backend.inventory.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompetitionMarketServiceTest {
    ObservationRepository observations = mock(ObservationRepository.class);
    FuelPriceQuery own = mock(FuelPriceQuery.class);
    MarketPriceService market = new MarketPriceService(observations, own);
    Instant asOf = Instant.parse("2020-01-02T02:00:00Z");
    @BeforeEach void setup() {
        when(observations.findLatest(null, FuelType.MOTOR, asOf, false)).thenReturn(List.of(
                new PriceObservation(1L, 1L, FuelType.MOTOR, new BigDecimal("100"), asOf, asOf),
                new PriceObservation(2L, 2L, FuelType.MOTOR, new BigDecimal("120"), asOf, asOf)));
    }
    @Test void queriesPublicInventoryContractUsingTheBogotaDate() {
        when(own.findEffectivePrice(FuelType.MOTOR, LocalDate.of(2020,1,1))).thenReturn(Optional.of(
                new FuelPriceInfo(FuelType.MOTOR, new BigDecimal("121"), LocalDate.of(2019,12,1))));
        var result = market.compare(FuelType.MOTOR, asOf);
        assertThat(result.mode()).isEqualTo("AS_OF"); assertThat(result.bellavistaDate()).isEqualTo(LocalDate.of(2020,1,1));
        assertThat(result.fuels().getFirst().versusAverage().percentage()).isEqualByComparingTo("10");
        assertThat(result.fuels().getFirst().versusMinimum().absoluteCopPerGallon()).isEqualByComparingTo("21");
        verify(own).findEffectivePrice(FuelType.MOTOR, LocalDate.of(2020,1,1));
    }
    @Test void missingBellavistaRetainsCompetitorDataWithoutInventingDifferences() {
        var result = market.compare(FuelType.MOTOR, asOf).fuels().getFirst();
        assertThat(result.bellavistaPrice()).isNull(); assertThat(result.competition().competitorCount()).isEqualTo(2);
        assertThat(result.versusAverage().percentage()).isNull();
    }
    @Test void missingMarketRetainsAvailableBellavistaPrice() {
        when(observations.findLatest(null, FuelType.MOTOR, asOf, false)).thenReturn(List.of());
        when(own.findEffectivePrice(any(), any())).thenReturn(Optional.of(new FuelPriceInfo(FuelType.MOTOR, BigDecimal.TEN, LocalDate.of(2020,1,1))));
        var result = market.compare(FuelType.MOTOR, asOf).fuels().getFirst();
        assertThat(result.bellavistaPrice()).isEqualByComparingTo("10"); assertThat(result.competition().average()).isNull();
    }
    @Test void simulationUsesExplicitCostAndNeverUsesInventoryAsCost() {
        var result = market.simulate(FuelType.MOTOR, new BigDecimal("121"), new BigDecimal("96.80"), asOf);
        assertThat(result.versusAverage().percentage()).isEqualByComparingTo("10");
        assertThat(result.estimatedMargin().grossMarginPercentage()).isEqualByComparingTo("20");
        verifyNoInteractions(own);
    }
    @Test void simulationWithoutCostLeavesFinancialMarginUnavailable() {
        var result = market.simulate(FuelType.MOTOR, new BigDecimal("121"), null, asOf);
        assertThat(result.estimatedMargin().grossCopPerGallon()).isNull();
        assertThat(result.competition().minimum()).isEqualByComparingTo("100"); verifyNoInteractions(own);
    }
    @Test void allFuelComparisonPreservesMissingDataPerFuel() {
        when(observations.findLatest(null, null, asOf, false)).thenReturn(List.of());
        assertThat(market.compare(null, asOf).fuels()).hasSize(FuelType.values().length)
                .allSatisfy(f -> assertThat(f.competition().competitorCount()).isZero());
    }
}
