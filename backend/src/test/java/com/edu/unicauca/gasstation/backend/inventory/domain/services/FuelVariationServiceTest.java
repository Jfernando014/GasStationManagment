package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelReceptionRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.FuelTestData.tank;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reception;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelVariationServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private TankRepository tankRepository;
    @Mock
    private TankMeasurementRepository measurementRepository;
    @Mock
    private FuelReceptionRepository receptionRepository;
    @Mock
    private SalesSummaryPort salesSummaryPort;
    @Mock
    private GaugeTableService gaugeTableService;

    @InjectMocks
    private FuelVariationService service;

    // ---------- variation ----------

    @Test
    void realDataOfAugust5ShowsNoVariation() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"), previousMeasurement());
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getFuelType()).isEqualTo(FuelType.MOTOR);
        assertThat(variation.getStatus()).isEqualTo(VariationStatus.OK);
        assertThat(variation.getTankIds()).containsExactly(1L, 2L, 3L);
        assertThat(variation.getInitialGallons()).isEqualByComparingTo("1558.05");
        assertThat(variation.getEntriesGallons()).isEqualByComparingTo("3970");
        assertThat(variation.getSoldGallons()).isEqualByComparingTo("784.587");
        assertThat(variation.getTheoreticalGallons()).isEqualByComparingTo("4743.463");
        assertThat(variation.getMeasuredGallons()).isEqualByComparingTo("4743.463");
        assertThat(variation.getVarianceGallons()).isEqualByComparingTo("0");
        assertThat(variation.isOutOfTolerance()).isFalse();
    }

    @Test
    void varianceIsMeasuredMinusTheoreticalSoAShortageIsNegative() {
        givenTanks(motorTanks("0"));
        givenMeasurements(prototypeMeasurement(), previousMeasurement());
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getMeasuredGallons()).isEqualByComparingTo("4703.60");
        assertThat(variation.getVarianceGallons()).isEqualByComparingTo("-39.863");
        assertThat(variation.isOutOfTolerance()).isTrue(); // zero tolerance flags any variance
    }

    @Test
    void noReceptionsMeansZeroEntries() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"), previousMeasurement());
        givenReceptions();
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getEntriesGallons()).isEqualByComparingTo("0");
        assertThat(variation.getTheoreticalGallons()).isEqualByComparingTo("773.463");
    }

    @Test
    void entriesAndVariationAreCalculatedPerFuel() {
        givenTanks(List.of(tank(1L, "T1C1", FuelType.MOTOR, "0"), tank(4L, "T3", FuelType.DIESEL, "0")));
        givenMeasurements(
                measurement(1L, DATE, reading(1L, "100", "1000.00"), reading(4L, "100", "500.00")),
                measurement(2L, DATE.minusDays(1), reading(1L, "100", "900.00"), reading(4L, "100", "450.00")));
        givenReceptions(reception(1L, 1L, DATE, "100"), reception(2L, 4L, DATE, "50"),
                reception(3L, 1L, DATE, "20"));
        givenSales(Map.of(FuelType.MOTOR, new BigDecimal("80"), FuelType.DIESEL, new BigDecimal("30")));

        List<FuelVariation> variations = service.calculate(DATE);

        assertThat(variations).extracting(FuelVariation::getFuelType)
                .containsExactly(FuelType.MOTOR, FuelType.DIESEL);
        assertThat(variations.get(0).getEntriesGallons()).isEqualByComparingTo("120");
        assertThat(variations.get(0).getVarianceGallons()).isEqualByComparingTo("60");
        assertThat(variations.get(1).getEntriesGallons()).isEqualByComparingTo("50");
        assertThat(variations.get(1).getVarianceGallons()).isEqualByComparingTo("30");
    }

    @Test
    void onlyTheImmediatelyPreviousDateIsUsedAsTheInitialStock() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"), previousMeasurement());
        givenReceptions();
        givenSold("784.587");

        service.calculate(DATE);

        verify(measurementRepository).findByJournalDate(DATE.minusDays(1));
        verify(measurementRepository, never()).findLatestBefore(any());
    }

    // ---------- tolerance ----------

    @Test
    void varianceBeyondTheToleranceIsFlagged() {
        givenTanks(motorTanks("1"));
        givenMeasurements(prototypeMeasurement(), previousMeasurement());
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");
        givenToleranceSpanOfEveryMotorTank("20"); // half of 20 per tank = 30 gallons in total

        FuelVariation variation = onlyFuel();

        assertThat(variation.getToleranceGallons()).isEqualByComparingTo("30");
        assertThat(variation.getVarianceGallons()).isEqualByComparingTo("-39.863");
        assertThat(variation.isOutOfTolerance()).isTrue();
    }

    @Test
    void varianceWithinTheToleranceIsNotFlagged() {
        givenTanks(motorTanks("1"));
        givenMeasurements(prototypeMeasurement(), previousMeasurement());
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");
        givenToleranceSpanOfEveryMotorTank("40"); // 60 gallons in total

        FuelVariation variation = onlyFuel();

        assertThat(variation.getToleranceGallons()).isEqualByComparingTo("60");
        assertThat(variation.isOutOfTolerance()).isFalse();
    }

    @Test
    void varianceEqualToTheToleranceIsNotFlagged() {
        givenTanks(motorTanks("1"));
        givenMeasurements(exactMeasurement("1213.463"), previousMeasurement()); // variance -30
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");
        givenToleranceSpanOfEveryMotorTank("20");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getVarianceGallons()).isEqualByComparingTo("-30");
        assertThat(variation.getToleranceGallons()).isEqualByComparingTo("30");
        assertThat(variation.isOutOfTolerance()).isFalse();
    }

    @Test
    void toleranceFallsBackToTheMeasuredHeightWhenTheUpperBoundExceedsTheGauge() {
        givenTanks(List.of(tank(1L, "T1C1", FuelType.MOTOR, "1"), tank(2L, "T1C2", FuelType.MOTOR, "0"),
                tank(3L, "T2C1", FuelType.MOTOR, "0")));
        givenMeasurements(prototypeMeasurement(), previousMeasurement());
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");
        when(gaugeTableService.calculateVolume(eq(1L), heightEq("150.4")))
                .thenThrow(new HeightExceedsGaugeException(new BigDecimal("149.4")));
        when(gaugeTableService.calculateVolume(eq(1L), heightEq("149.4"))).thenReturn(new BigDecimal("1010.00"));
        when(gaugeTableService.calculateVolume(eq(1L), heightEq("148.4"))).thenReturn(new BigDecimal("1000.00"));

        FuelVariation variation = onlyFuel();

        assertThat(variation.getToleranceGallons()).isEqualByComparingTo("5"); // half of (1010 - 1000)
    }

    // ---------- statuses ----------

    @Test
    void incompleteWhenThereIsNoMeasurementToday() {
        givenTanks(motorTanks("0"));
        givenMeasurements(null, previousMeasurement());
        givenReceptions();
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getStatus()).isEqualTo(VariationStatus.INCOMPLETE_MEASUREMENT);
        assertThat(variation.getMeasuredGallons()).isNull();
        assertThat(variation.getInitialGallons()).isEqualByComparingTo("1558.05");
        assertThat(variation.getTheoreticalGallons()).isNull();
        assertThat(variation.getVarianceGallons()).isNull();
        assertThat(variation.isOutOfTolerance()).isFalse();
    }

    @Test
    void incompleteWhenATankOfTheFuelWasNotMeasuredToday() {
        givenTanks(motorTanks("0"));
        givenMeasurements(
                measurement(1L, DATE, reading(1L, "149.4", "2000.000"), reading(2L, "125.4", "1500.000")),
                previousMeasurement());
        givenReceptions();
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getStatus()).isEqualTo(VariationStatus.INCOMPLETE_MEASUREMENT);
        assertThat(variation.getMeasuredGallons()).isNull();
        assertThat(variation.getVarianceGallons()).isNull();
    }

    @Test
    void noPreviousMeasurementWhenTheDayBeforeWasNotMeasured() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"), null);
        givenReceptions(reception(1L, 1L, DATE, "3970"));
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getStatus()).isEqualTo(VariationStatus.NO_PREVIOUS_MEASUREMENT);
        assertThat(variation.getInitialGallons()).isNull();
        assertThat(variation.getMeasuredGallons()).isEqualByComparingTo("4743.463");
        assertThat(variation.getEntriesGallons()).isEqualByComparingTo("3970");
        assertThat(variation.getVarianceGallons()).isNull();
    }

    @Test
    void incompleteWhenTheDayBeforeLacksATankOfTheFuel() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"),
                measurement(2L, DATE.minusDays(1), reading(1L, "100", "600.00"), reading(2L, "100", "500.00")));
        givenReceptions();
        givenSold("784.587");

        FuelVariation variation = onlyFuel();

        assertThat(variation.getStatus()).isEqualTo(VariationStatus.INCOMPLETE_MEASUREMENT);
        assertThat(variation.getInitialGallons()).isNull();
        assertThat(variation.getMeasuredGallons()).isEqualByComparingTo("4743.463");
    }

    @Test
    void noSalesDataWhenTheFuelHasNoSales() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"), previousMeasurement());
        givenReceptions();
        givenSales(Map.of(FuelType.DIESEL, new BigDecimal("10")));

        FuelVariation variation = onlyFuel();

        assertThat(variation.getStatus()).isEqualTo(VariationStatus.NO_SALES_DATA);
        assertThat(variation.getSoldGallons()).isNull();
        assertThat(variation.getInitialGallons()).isEqualByComparingTo("1558.05");
        assertThat(variation.getMeasuredGallons()).isEqualByComparingTo("4743.463");
        assertThat(variation.getVarianceGallons()).isNull();
    }

    @Test
    void noSalesDataWhenSalesDoNotProvideTheJournal() {
        givenTanks(motorTanks("0"));
        givenMeasurements(exactMeasurement("1243.463"), previousMeasurement());
        givenReceptions();
        when(salesSummaryPort.findByJournalDate(DATE)).thenReturn(Optional.empty());

        assertThat(onlyFuel().getStatus()).isEqualTo(VariationStatus.NO_SALES_DATA);
    }

    // ---------- helpers ----------

    private FuelVariation onlyFuel() {
        List<FuelVariation> variations = service.calculate(DATE);
        assertThat(variations).hasSize(1);
        return variations.get(0);
    }

    private List<Tank> motorTanks(String toleranceCm) {
        return List.of(tank(1L, "T1C1", FuelType.MOTOR, toleranceCm),
                tank(2L, "T1C2", FuelType.MOTOR, toleranceCm),
                tank(3L, "T2C1", FuelType.MOTOR, toleranceCm));
    }

    /** Net gallons add up to 1558.05 gal, the real initial stock of Corriente on 05/08. */
    private TankMeasurement previousMeasurement() {
        return measurement(2L, DATE.minusDays(1),
                reading(1L, "100", "600.00"), reading(2L, "100", "500.00"), reading(3L, "100", "458.05"));
    }

    private TankMeasurement exactMeasurement(String thirdTankGallons) {
        return measurement(1L, DATE,
                reading(1L, "149.4", "2000.000"), reading(2L, "125.4", "1500.000"),
                reading(3L, "75.3", thirdTankGallons));
    }

    /** Prototype heights, gauge volume minus 20 gal per tank: 4703.60 gal in total. */
    private TankMeasurement prototypeMeasurement() {
        return measurement(1L, DATE,
                reading(1L, "149.4", "2130.00"), reading(2L, "125.4", "1719.80"), reading(3L, "75.3", "853.80"));
    }

    private void givenTanks(List<Tank> tanks) {
        when(tankRepository.findAllOrderByCode()).thenReturn(tanks);
    }

    private void givenMeasurements(TankMeasurement current, TankMeasurement previous) {
        when(measurementRepository.findByJournalDate(DATE)).thenReturn(Optional.ofNullable(current));
        when(measurementRepository.findByJournalDate(DATE.minusDays(1))).thenReturn(Optional.ofNullable(previous));
    }

    private void givenReceptions(FuelReception... receptions) {
        when(receptionRepository.search(null, DATE, DATE)).thenReturn(List.of(receptions));
    }

    private void givenSold(String motorGallons) {
        givenSales(Map.of(FuelType.MOTOR, new BigDecimal(motorGallons)));
    }

    private void givenSales(Map<FuelType, BigDecimal> gallonsByFuel) {
        when(salesSummaryPort.findByJournalDate(DATE))
                .thenReturn(Optional.of(new SalesSummary(DATE, gallonsByFuel)));
    }

    private void givenToleranceSpanOfEveryMotorTank(String span) {
        givenToleranceSpan(1L, "149.4", span);
        givenToleranceSpan(2L, "125.4", span);
        givenToleranceSpan(3L, "75.3", span);
    }

    /** The gauge volume 1 cm above the height is {@code span} gallons more than 1 cm below it. */
    private void givenToleranceSpan(Long tankId, String height, String span) {
        BigDecimal measured = new BigDecimal(height);
        when(gaugeTableService.calculateVolume(eq(tankId), heightEq(measured.add(BigDecimal.ONE).toPlainString())))
                .thenReturn(new BigDecimal("1000.00").add(new BigDecimal(span)));
        when(gaugeTableService.calculateVolume(eq(tankId), heightEq(measured.subtract(BigDecimal.ONE).toPlainString())))
                .thenReturn(new BigDecimal("1000.00"));
    }

    private static BigDecimal heightEq(String value) {
        BigDecimal expected = new BigDecimal(value);
        return argThat(height -> height != null && height.compareTo(expected) == 0);
    }
}