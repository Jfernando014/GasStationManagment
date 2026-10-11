package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesHistoryReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesHistoryReadPort.DailySale;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SalesTargetHistoricalDayAveragesTest {
    private final LocalDate from=LocalDate.of(2026,8,1),to=LocalDate.of(2026,8,31);
    @Test
    void explicitFixturePortExcludesZerosAndLeavesMissingWeekdaysUnavailable() {
        SalesHistoryReadPort fixture=(f,t,c) -> List.of(new DailySale(LocalDate.of(2026,8,3),new BigDecimal("1")),
                new DailySale(LocalDate.of(2026,8,10),new BigDecimal("2")),new DailySale(LocalDate.of(2026,8,17),BigDecimal.ZERO));
        var result=new HistoricalDayAverages(fixture).read(from,to,SalesTargetCategory.FUEL_AGGREGATE);
        assertThat(result.get(DayOfWeek.MONDAY)).contains(ExactFraction.of(new BigDecimal("1.5")));
        assertThat(result.get(DayOfWeek.TUESDAY)).isEmpty();
        assertThat(new HistoricalDayAverages((f,t,c)->List.of()).read(from,to,SalesTargetCategory.BASKET).values()).allSatisfy(v->assertThat(v).isEmpty());
    }
    @Test
    void repeatingAverageRemainsExactAndBadSourceDataIsRejected() {
        SalesHistoryReadPort fixture=(f,t,c) -> List.of(new DailySale(LocalDate.of(2026,8,3),BigDecimal.ONE),
                new DailySale(LocalDate.of(2026,8,10),BigDecimal.ONE),new DailySale(LocalDate.of(2026,8,17),new BigDecimal("2")));
        assertThat(new HistoricalDayAverages(fixture).read(from,to,SalesTargetCategory.FUEL_AGGREGATE).get(DayOfWeek.MONDAY))
                .contains(new ExactFraction(java.math.BigInteger.valueOf(4),java.math.BigInteger.valueOf(3)));
        for(var rows:List.of(List.of(new DailySale(from,new BigDecimal("-1"))),
                List.of(new DailySale(from,BigDecimal.ONE),new DailySale(from,BigDecimal.ONE)),List.of(new DailySale(to.plusDays(1),BigDecimal.ONE)))) {
            assertThatThrownBy(()->new HistoricalDayAverages((f,t,c)->rows).read(from,to,SalesTargetCategory.FUEL_AGGREGATE))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(()->new HistoricalDayAverages(fixture).read(to,from,SalesTargetCategory.BASKET)).isInstanceOf(IllegalArgumentException.class);
    }
}
