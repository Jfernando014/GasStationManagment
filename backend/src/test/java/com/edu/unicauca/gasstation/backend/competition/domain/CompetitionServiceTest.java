package com.edu.unicauca.gasstation.backend.competition.domain;

import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.*;
import com.edu.unicauca.gasstation.backend.competition.domain.services.CompetitionService;
import com.edu.unicauca.gasstation.backend.competition.exception.*;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompetitionServiceTest {
    CompetitorRepository competitors = mock(CompetitorRepository.class);
    ObservationRepository observations = mock(ObservationRepository.class);
    CompetitionService service = new CompetitionService(competitors, observations);
    @BeforeEach void setup() {
        when(competitors.findByIdForUpdate(1)).thenReturn(Optional.of(new Competitor(1L, "Station", "Brand", "Zone", true)));
        when(competitors.save(any())).thenAnswer(call -> call.getArgument(0));
        when(observations.append(any())).thenAnswer(call -> call.getArgument(0));
    }
    @Test void registersAndNormalizesIdentity() {
        var value = service.register(" Station ", " Brand ", " Zone ");
        assertThat(value.name()).isEqualTo("Station"); assertThat(value.active()).isTrue();
    }
    @Test void updatingIdentityPreservesInactiveStatus() {
        when(competitors.findByIdForUpdate(1)).thenReturn(Optional.of(new Competitor(1L, "Old", "Brand", "Zone", false)));
        assertThat(service.update(1, "New", "Other", "There").active()).isFalse();
    }
    @Test void deactivationNeverDeletesObservations() {
        assertThat(service.changeStatus(1, false).active()).isFalse(); verifyNoInteractions(observations);
    }
    @Test void recordsTheProvidedHistoricalInstantInsteadOfTheRegistrationDate() {
        var date = Instant.parse("2020-01-01T01:02:03.123456Z");
        var value = service.observe(1, FuelType.MAX_PRO, new BigDecimal("123.45"), date);
        assertThat(value.observedAt()).isEqualTo(date); assertThat(value.createdAt()).isNull();
    }
    @Test void inactiveCompetitorsCannotReceiveNewObservations() {
        when(competitors.findByIdForUpdate(1)).thenReturn(Optional.of(new Competitor(1L, "Old", "Brand", "Zone", false)));
        assertThatThrownBy(() -> service.observe(1, FuelType.MOTOR, BigDecimal.ONE, Instant.parse("2020-01-01T00:00:00Z")))
                .isInstanceOf(InactiveCompetitorException.class);
        verifyNoInteractions(observations);
    }
    @Test void rejectsUnknownCompetitorAndNonpositiveIdentifiers() {
        assertThatThrownBy(() -> service.find(2)).isInstanceOf(CompetitorNotFoundException.class);
        assertThatThrownBy(() -> service.find(0)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsMissingAndFutureObservationDates() {
        assertThatThrownBy(() -> service.observe(1, FuelType.MOTOR, BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.observe(1, FuelType.MOTOR, BigDecimal.ONE, Instant.parse("9999-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(observations);
    }
    @Test void rejectsInvalidPeriodsAndPagination() {
        var date = Instant.parse("2020-01-01T00:00:00Z");
        assertThatThrownBy(() -> service.history(null, null, date.plusSeconds(1), date, 0, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.history(null, null, null, null, -1, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.history(null, null, null, null, 0, 201)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(observations);
    }
}
