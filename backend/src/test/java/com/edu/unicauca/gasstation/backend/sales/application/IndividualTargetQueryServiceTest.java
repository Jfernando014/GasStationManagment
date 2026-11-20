package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.IndividualTargetQuery.Measure;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetIdentity;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DistributionProfileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IndividualTargetQueryServiceTest {

    private final SalesTargetService targets = mock(SalesTargetService.class);
    private final DistributionProfileRepository profiles = mock(DistributionProfileRepository.class);
    private final DayStaffQuery staff = mock(DayStaffQuery.class);
    private final IndividualTargetQueryService service = new IndividualTargetQueryService(targets, profiles, staff);

    @Test
    void aPeriodThatIsNotExactlyOneCalendarMonthHasNoTargetsAndNothingIsRead() {
        var targetsOfADay = service.findIndividualTargets(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15), Measure.FUEL_GALLONS);

        assertThat(targetsOfADay).isEmpty();
        verifyNoInteractions(targets, profiles, staff);
    }

    @Test
    void aMonthWithoutARegisteredTargetHasNoTargetsInsteadOfAZeroTarget() {
        when(targets.findByIdentity(any(SalesTargetIdentity.class))).thenReturn(Optional.empty());

        var result = service.findIndividualTargets(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), Measure.BASKET_COP);

        assertThat(result).isEmpty();
        verifyNoInteractions(staff);
    }
}
