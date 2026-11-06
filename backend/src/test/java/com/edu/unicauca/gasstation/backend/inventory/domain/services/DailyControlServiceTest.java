package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.DailyControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.LocationStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.autonomy;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.DIESEL;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.EXTRA;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyControlServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private FuelVariationService variationService;
    @Mock
    private FuelAutonomyService autonomyService;
    @Mock
    private ProductStockService productStockService;

    private DailyControlService service;

    @BeforeEach
    void setUp() {
        service = new DailyControlService(variationService, autonomyService, productStockService);
        when(productStockService.findByJournalDate(DATE, null)).thenReturn(List.of());
    }

    @Test
    void joinsTheVariationAndTheAutonomyOfEachFuel() {
        when(variationService.calculate(DATE)).thenReturn(List.of(variation(MOTOR), variation(DIESEL)));
        // reversed on purpose: the join is by fuel, not by position
        when(autonomyService.calculate(DATE)).thenReturn(List.of(
                autonomy(DIESEL, AutonomyStatus.CALCULATED, AutonomyLevel.OK, "7.00"),
                autonomy(MOTOR, AutonomyStatus.CALCULATED, AutonomyLevel.WARNING, "4.00")));

        DailyControl control = service.findByJournalDate(DATE);

        assertThat(control.getJournalDate()).isEqualTo(DATE);
        assertThat(control.getFuels()).extracting(FuelControl::getFuelType).containsExactly(MOTOR, DIESEL);
        FuelControl motor = control.getFuels().get(0);
        assertThat(motor.getVariation().getFuelType()).isEqualTo(MOTOR);
        assertThat(motor.getAutonomy().getLevel()).isEqualTo(AutonomyLevel.WARNING);
        assertThat(control.getFuels().get(1).getAutonomy().getAutonomyDays()).isEqualByComparingTo("7");
    }

    @ParameterizedTest
    @CsvSource({"3.75,3", "0.99,0", "5.00,5"})
    void theDepletionDateAddsTheWholeDaysOfAutonomy(String days, long expectedDays) {
        when(variationService.calculate(DATE)).thenReturn(List.of(variation(MOTOR)));
        when(autonomyService.calculate(DATE)).thenReturn(List.of(
                autonomy(MOTOR, AutonomyStatus.CALCULATED, AutonomyLevel.OK, days)));

        FuelControl motor = service.findByJournalDate(DATE).getFuels().get(0);

        assertThat(motor.getEstimatedDepletionDate()).isEqualTo(DATE.plusDays(expectedDays));
    }

    @Test
    void thereIsNoDepletionDateWhenTheAutonomyWasNotCalculated() {
        when(variationService.calculate(DATE)).thenReturn(List.of(variation(MOTOR)));
        when(autonomyService.calculate(DATE)).thenReturn(List.of(
                autonomy(MOTOR, AutonomyStatus.NO_SALES_HISTORY, AutonomyLevel.NOT_EVALUATED, null)));

        assertThat(service.findByJournalDate(DATE).getFuels().get(0).getEstimatedDepletionDate()).isNull();
    }

    @Test
    void aFuelWithoutAutonomyResultStillAppears() {
        when(variationService.calculate(DATE)).thenReturn(List.of(variation(EXTRA)));
        when(autonomyService.calculate(DATE)).thenReturn(List.of());

        FuelControl extra = service.findByJournalDate(DATE).getFuels().get(0);

        assertThat(extra.getAutonomy()).isNull();
        assertThat(extra.getEstimatedDepletionDate()).isNull();
    }

    @Test
    void theProductsCarryTheSoldUnitsAndTheClosingStockOfEachLocation() {
        givenNoFuels();
        when(productStockService.findByJournalDate(DATE, null)).thenReturn(List.of(
                stock(1L, "80220", 32, 3, 10, StockStatus.OK)));

        ProductControl product = service.findByJournalDate(DATE).getProducts().get(0);

        assertThat(product.getProductCode()).isEqualTo("80220");
        assertThat(product.getSold()).isEqualTo(3);
        assertThat(product.getOfficeStock()).isEqualTo(32);
        assertThat(product.getCounterStock()).isEqualTo(10);
        assertThat(product.getStatus()).isEqualTo(StockStatus.OK);
        assertThat(product.isDepleted()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"0,0,true", "5,0,false", "0,4,false", "0,-2,true", "5,-2,false"})
    void aProductIsDepletedWhenTheTotalIsZeroOrLess(int office, int counter, boolean depleted) {
        givenNoFuels();
        when(productStockService.findByJournalDate(DATE, null)).thenReturn(List.of(
                stock(1L, "80220", office, 0, counter, counter < 0 ? StockStatus.NEGATIVE_STOCK : StockStatus.OK)));

        assertThat(service.findByJournalDate(DATE).getProducts().get(0).isDepleted()).isEqualTo(depleted);
    }

    @Test
    void itAsksForTheProductsOfAllTheCategories() {
        givenNoFuels();

        service.findByJournalDate(DATE);

        verify(productStockService).findByJournalDate(DATE, null);
    }

    // ---------- helpers ----------

    private void givenNoFuels() {
        when(variationService.calculate(DATE)).thenReturn(List.of());
        when(autonomyService.calculate(DATE)).thenReturn(List.of());
    }

    private FuelVariation variation(com.edu.unicauca.gasstation.backend.inventory.FuelType fuelType) {
        return FuelVariation.builder().fuelType(fuelType).status(VariationStatus.OK).build();
    }

    private ProductStock stock(Long id, String code, int office, int sold, int counter, StockStatus status) {
        return ProductStock.builder()
                .productId(id)
                .productCode(code)
                .productName("Product " + code)
                .category(ProductCategory.OTRO)
                .active(true)
                .office(LocationStock.builder().closing(office).build())
                .counter(LocationStock.builder().sold(sold).closing(counter).build())
                .status(status)
                .build();
    }
}