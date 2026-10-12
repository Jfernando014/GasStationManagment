package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class SalesTargetDistributionEngineTest {
    private final SalesTargetDistributionEngine engine = new SalesTargetDistributionEngine();
    @Test
    void matchesOriginalWorkbookCachedSharesUsingOriginalParametersAndSchedule() {
        var result=engine.calculate(new Input(SalesTargetTestData.fuel(8,"26920"),SalesTargetDistributionTestData.workbook(),
                SalesTargetDistributionTestData.workbookSchedule(),12));
        // CALC METAS C4:C7/H4. Workbook caches have finite displayed precision; never production assertions.
        var expected=List.of("0.2754425694","0.2693149571","0.2726738509","0.1825686227");
        for(int i=0;i<4;i++) {
            var target=new BigDecimal(expected.get(i)).multiply(new BigDecimal("26920"));
            assertThat(result.byWorker().get(new UUID(0,i+1)).official()).isCloseTo(target,within(new BigDecimal("0.00001")));
        }
        assertThat(result.byWorker().get(new UUID(0,1)).reference())
                .isCloseTo(new BigDecimal("0.2573270561").multiply(new BigDecimal("26920")),within(new BigDecimal("0.00001")));
    }
    @Test
    void largestRemaindersConserveRepeatingThirdsAndTieOrder() {
        var thirds = Collections.nCopies(3, ExactFraction.of(BigDecimal.ONE));
        assertThat(ConservingAllocation.allocate(BigDecimal.ONE, thirds, 2))
                .containsExactly(new BigDecimal("0.34"), new BigDecimal("0.33"), new BigDecimal("0.33"));
        assertThat(ConservingAllocation.allocate(new BigDecimal("0.01"), thirds, 2))
                .containsExactly(new BigDecimal("0.01"), new BigDecimal("0.00"), new BigDecimal("0.00"));
    }
    @Test
    void explicitSharesMatchExcelOfficialAndReferenceWithSingleAndPairedWorkers() {
        var date = LocalDate.of(2026,8,1);
        var a = new Coverage(new Assignment(date,new UUID(0,1),"DIA"), Set.of(12));
        var b = new Coverage(new Assignment(date,new UUID(0,2),"12-7"), Set.of(12));
        var result = engine.calculate(new Input(SalesTargetTestData.fuel(8,"100"), SalesTargetDistributionTestData.workbook(), List.of(a,b), 2));
        assertThat(result.byWorker().get(a.assignment().workerId()).official()).isEqualByComparingTo("70");
        assertThat(result.byWorker().get(b.assignment().workerId()).official()).isEqualByComparingTo("30");
        assertThat(result.byWorker().get(a.assignment().workerId()).reference()).isEqualByComparingTo("50");
        var single = engine.calculate(new Input(SalesTargetTestData.fuel(8,"100"), SalesTargetDistributionTestData.workbook(), List.of(b), 2));
        assertThat(single.byWorker().get(b.assignment().workerId()).official()).isEqualByComparingTo("100");
        assertThat(single.uncoveredHours()).isNotEmpty();
    }
    @Test
    void dailyAndHourlyProportionsUseHistoricalDayAndHourWeights() {
        var days = new EnumMap<DayOfWeek,BigDecimal>(DayOfWeek.class);
        for(var d:DayOfWeek.values()) days.put(d,BigDecimal.ONE);
        days.put(DayOfWeek.SUNDAY,new BigDecimal("2"));
        var weights = new ArrayList<>(Collections.nCopies(24,BigDecimal.ZERO));
        weights.set(0,BigDecimal.ONE); weights.set(1,new BigDecimal("3"));
        var p = new DistributionProfile("Fixture","Explicit synthetic fixture",weights,days,Set.of(),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.ZERO);
        var worker = new UUID(0,1);
        var coverage = List.of(new Coverage(new Assignment(LocalDate.of(2026,8,1),worker,"DIA"),Set.of(0,1)),
                new Coverage(new Assignment(LocalDate.of(2026,8,2),worker,"DIA"),Set.of(0,1)));
        var result = engine.calculate(new Input(SalesTargetTestData.fuel(8,"120"),p,coverage,2));
        assertThat(result.byDate().get(LocalDate.of(2026,8,1)).official()).isEqualByComparingTo("40");
        assertThat(result.byDate().get(LocalDate.of(2026,8,2)).official()).isEqualByComparingTo("80");
        assertThat(result.byHour().get(new HourKey(LocalDate.of(2026,8,1),0)).official()).isEqualByComparingTo("10");
        assertThat(result.byHour().get(new HourKey(LocalDate.of(2026,8,1),1)).official()).isEqualByComparingTo("30");
    }
    @Test
    void entireMonthConservesBothTargetsAndIsDeterministicUnderReorderedInput() {
        var coverage = SalesTargetDistributionTestData.fullMonth();
        var p = SalesTargetDistributionTestData.workbook();
        var original = engine.calculate(new Input(SalesTargetTestData.fuel(8,"26920"),p,coverage,12));
        var reversed = new ArrayList<>(coverage); Collections.reverse(reversed);
        assertThat(engine.calculate(new Input(SalesTargetTestData.fuel(8,"26920"),p,reversed,12))).isEqualTo(original);
        assertThat(original.uncoveredHours()).isEmpty();
        assertThat(original.byDate()).hasSize(31); assertThat(original.byHour()).hasSize(744);
        assertThat(original.allocations().stream().map(Allocation::official).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("26920");
        assertThat(original.byWorker().values().stream().map(Totals::reference).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("26920");
        var basket = new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.monthly(2026,8),SalesTargetCategory.BASKET),new BigDecimal("8400000"),SalesTargetUnit.COP);
        var canasta = engine.calculate(new Input(basket,p,coverage,0));
        assertThat(canasta.byWorker().values().stream().map(Totals::official).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("8400000");
    }
    @Test
    void rejectsMissingCoverageAnnualTargetDuplicateAssignmentsAndOutOfMonth() {
        var p=SalesTargetDistributionTestData.workbook(); var target=SalesTargetTestData.fuel(8,"1");
        assertThatThrownBy(() -> engine.calculate(new Input(target,p,List.of(),12))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.calculate(new Input(SalesTargetTestData.fuel(null,"1"),p,List.of(),12))).isInstanceOf(IllegalArgumentException.class);
        var c=new Coverage(new Assignment(LocalDate.of(2026,8,1),new UUID(0,1),"DIA"),Set.of(1));
        assertThatThrownBy(() -> engine.calculate(new Input(target,p,List.of(c,c),12))).isInstanceOf(IllegalArgumentException.class);
        var outside=new Coverage(new Assignment(LocalDate.of(2026,9,1),new UUID(0,1),"DIA"),Set.of(1));
        assertThatThrownBy(() -> engine.calculate(new Input(target,p,List.of(outside),12))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coverage(c.assignment(),Set.of(24))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void leapMonthZeroTargetAndZeroActiveSharesHaveExplicitBehavior() {
        var p=SalesTargetDistributionTestData.workbook();
        var target=new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.monthly(2024,2),SalesTargetCategory.FUEL_AGGREGATE),BigDecimal.ZERO,SalesTargetUnit.GALLONS);
        var c=new Coverage(new Assignment(LocalDate.of(2024,2,29),new UUID(0,1),"DIA"),Set.of(12));
        var result=engine.calculate(new Input(target,p,List.of(c),12));
        assertThat(result.monthlyTotal().official()).isZero();
        assertThat(result.allocations()).allSatisfy(a->assertThat(a.official()).isZero());
        assertThat(result.byDate()).containsOnlyKeys(LocalDate.of(2024,2,29));
        var noTitular=new DistributionProfile("fixture","explicit zero share",p.hourlyWeights(),p.historicalDailyGallons(),p.supportShiftCodes(),
                BigDecimal.ZERO,BigDecimal.ONE,p.referenceTitularShare(),p.referenceSupportShare());
        assertThatThrownBy(()->engine.calculate(new Input(target,noTitular,List.of(c),12))).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(ints={-1,13})
    void rejectsInvalidPrecision(int scale) {
        assertThatThrownBy(() -> ConservingAllocation.allocate(BigDecimal.ONE,List.of(ExactFraction.of(BigDecimal.ONE)),scale)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void rejectsLossyTargetAndInvalidWeights() {
        var one=List.of(ExactFraction.of(BigDecimal.ONE));
        assertThatThrownBy(() -> ConservingAllocation.allocate(new BigDecimal("0.001"),one,2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConservingAllocation.allocate(BigDecimal.ONE,List.of(ExactFraction.ZERO),2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConservingAllocation.allocate(BigDecimal.ONE,List.of(ExactFraction.of(new BigDecimal("-1"))),2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConservingAllocation.allocate(new BigDecimal("-1"),one,2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExactFraction(BigInteger.ONE,BigInteger.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ExactFraction.of(new BigDecimal("1E3"))).isEqualTo(ExactFraction.of(new BigDecimal("1000")));
    }
    @Test
    void validatesProfileCompletenessSharesAndPositiveSums() {
        var p=SalesTargetDistributionTestData.workbook();
        assertThatThrownBy(() -> new DistributionProfile("x","fixture",List.of(BigDecimal.ONE),p.historicalDailyGallons(),Set.of(),p.titularShare(),p.supportShare(),p.referenceTitularShare(),p.referenceSupportShare())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DistributionProfile("x","fixture",Collections.nCopies(24,BigDecimal.ZERO),p.historicalDailyGallons(),Set.of(),p.titularShare(),p.supportShare(),p.referenceTitularShare(),p.referenceSupportShare())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DistributionProfile("x","fixture",p.hourlyWeights(),Map.of(),Set.of(),p.titularShare(),p.supportShare(),p.referenceTitularShare(),p.referenceSupportShare())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DistributionProfile("x","fixture",p.hourlyWeights(),p.historicalDailyGallons(),Set.of(),BigDecimal.ONE,BigDecimal.ONE,p.referenceTitularShare(),p.referenceSupportShare())).isInstanceOf(IllegalArgumentException.class);
        var negative=new ArrayList<>(p.hourlyWeights());negative.set(0,new BigDecimal("-1"));
        assertThatThrownBy(() -> new DistributionProfile("x","fixture",negative,p.historicalDailyGallons(),Set.of(),p.titularShare(),p.supportShare(),p.referenceTitularShare(),p.referenceSupportShare())).isInstanceOf(IllegalArgumentException.class);
    }
}
