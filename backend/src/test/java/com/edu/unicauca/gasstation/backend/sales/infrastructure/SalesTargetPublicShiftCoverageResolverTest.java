package com.edu.unicauca.gasstation.backend.sales.infrastructure;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Assignment;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.adapters.PublicShiftCoverageResolver;
import com.edu.unicauca.gasstation.backend.shifts.catalog.*;
import com.edu.unicauca.gasstation.backend.shifts.spi.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesTargetPublicShiftCoverageResolverTest {
    private final ShiftExternalService shifts=mock(ShiftExternalService.class);
    private final ScheduleWorkerDirectory workers=mock(ScheduleWorkerDirectory.class);
    private final PublicShiftCoverageResolver resolver=new PublicShiftCoverageResolver(shifts,workers);
    private final UUID worker=new UUID(0,1), roleId=new UUID(0,2);
    private final RoleInfo role=new RoleInfo(roleId,"TITULAR",1);
    private final LocalDate date=LocalDate.of(2026,8,1);

    @Test
    void resolvesNightAndSplitSegmentsWithoutChangingWorksheetDate() {
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"fixture",roleId,true)));
        when(shifts.getActiveCatalog()).thenReturn(List.of(
                code("NOCHE",List.of(new ShiftCodeInfo.Segment(18,6)),role),
                code("6-9y5-9",List.of(new ShiftCodeInfo.Segment(6,9),new ShiftCodeInfo.Segment(17,21)),role)));
        var night=resolver.resolve(List.of(new Assignment(date,worker,"NOCHE"))).get(0);
        assertThat(night.hours()).containsExactlyInAnyOrder(0,1,2,3,4,5,18,19,20,21,22,23);
        assertThat(night.assignment().date()).isEqualTo(date);
        var split=resolver.resolve(List.of(new Assignment(date,worker,"6-9y5-9"))).get(0);
        assertThat(split.hours()).containsExactlyInAnyOrder(6,7,8,17,18,19,20);
    }
    @Test
    void rejectsMissingInactiveWorkersUnknownCodesAndMismatchedRoles() {
        var input=List.of(new Assignment(date,worker,"DIA"));
        when(shifts.getActiveCatalog()).thenReturn(List.of(code("DIA",List.of(new ShiftCodeInfo.Segment(6,18)),role)));
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of());
        assertThatThrownBy(() -> resolver.resolve(input)).isInstanceOf(IllegalArgumentException.class);
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"fixture",roleId,false)));
        assertThatThrownBy(() -> resolver.resolve(input)).isInstanceOf(IllegalArgumentException.class);
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"fixture",new UUID(0,3),true)));
        assertThatThrownBy(() -> resolver.resolve(input)).isInstanceOf(IllegalArgumentException.class);
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"fixture",roleId,true)));
        assertThatThrownBy(() -> resolver.resolve(List.of(new Assignment(date,worker,"UNKNOWN")))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void restProducesEmptyCoverageAndMalformedSegmentIsRejected() {
        when(workers.getWorkersByIds(List.of(worker))).thenReturn(Map.of(worker,new ScheduleWorkerInfo(worker,"fixture",roleId,true)));
        when(shifts.getActiveCatalog()).thenReturn(List.of(code("DESCANSO",List.of(),null)));
        assertThat(resolver.resolve(List.of(new Assignment(date,worker,"DESCANSO"))).get(0).hours()).isEmpty();
        when(shifts.getActiveCatalog()).thenReturn(List.of(code("DIA",List.of(new ShiftCodeInfo.Segment(6,6)),role)));
        assertThatThrownBy(() -> resolver.resolve(List.of(new Assignment(date,worker,"DIA")))).isInstanceOf(IllegalArgumentException.class);
    }
    private ShiftCodeInfo code(String code,List<ShiftCodeInfo.Segment> segments,RoleInfo role) {
        return new ShiftCodeInfo(new UUID(0,4),code,code,role,null,segments,0,false,null,true);
    }
}
