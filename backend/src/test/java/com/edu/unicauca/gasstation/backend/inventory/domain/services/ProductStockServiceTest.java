package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.LocationStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.SalesSyncResult;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockBalance;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.ProductRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.StockMovementRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidStockMovementException;
import com.edu.unicauca.gasstation.backend.inventory.exception.NoSalesDataException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProductNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.StockConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.StockTestData.movement;
import static com.edu.unicauca.gasstation.backend.inventory.StockTestData.product;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation.COUNTER;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation.OFFICE;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType.ADJUSTMENT;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType.ENTRY;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType.INITIAL;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType.SALE;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType.TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-05T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final String INITIAL_INVALID = "The initial stock must be a whole number greater than or equal to zero";

    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SalesSummaryPort salesSummaryPort;

    @Captor
    private ArgumentCaptor<List<StockMovement>> movementsCaptor;

    private ProductStockService service;

    @BeforeEach
    void setUp() {
        service = new ProductStockService(movementRepository, productRepository, salesSummaryPort, CLOCK);
    }

    // ---------- initial stock ----------

    @Test
    void initialStockSavesBothLocationsTogether() {
        givenUsableProduct();
        givenSavingEchoes();

        service.registerInitial(1L, 12, 5, DATE);

        List<StockMovement> saved = savedMovements();
        assertThat(saved).extracting(StockMovement::getLocation).containsExactly(OFFICE, COUNTER);
        assertThat(saved).extracting(StockMovement::getQuantity).containsExactly(12, 5);
        assertThat(saved).allSatisfy(movement -> {
            assertThat(movement.getMovementType()).isEqualTo(INITIAL);
            assertThat(movement.getProductId()).isEqualTo(1L);
            assertThat(movement.getJournalDate()).isEqualTo(DATE);
            assertThat(movement.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 0));
        });
    }

    @Test
    void initialStockAcceptsZero() {
        givenUsableProduct();
        givenSavingEchoes();

        service.registerInitial(1L, 0, 0, DATE);

        assertThat(savedMovements()).extracting(StockMovement::getQuantity).containsExactly(0, 0);
    }

    @ParameterizedTest
    @CsvSource({"-1,5", "5,-1"})
    void initialStockRejectsANegativeQuantityAndSavesNothing(int office, int counter) {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerInitial(1L, office, counter, DATE))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage(INITIAL_INVALID);
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void initialStockRejectsAnOfficeThatWasAlreadyInitialized() {
        givenUsableProduct();
        when(movementRepository.existsInitial(1L, OFFICE)).thenReturn(true);

        assertThatThrownBy(() -> service.registerInitial(1L, 1, 1, DATE))
                .isInstanceOf(StockConflictException.class)
                .hasMessage("The initial stock of the product was already registered");
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void initialStockRejectsACounterThatWasAlreadyInitialized() {
        givenUsableProduct();
        when(movementRepository.existsInitial(1L, COUNTER)).thenReturn(true);

        assertThatThrownBy(() -> service.registerInitial(1L, 1, 1, DATE))
                .isInstanceOf(StockConflictException.class);
    }

    // ---------- common rules ----------

    @Test
    void movementsRejectAnUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerEntry(99L, 5, DATE, null))
                .isInstanceOf(ProductNotFoundException.class);
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void movementsRejectAnInactiveProduct() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, false)));

        assertThatThrownBy(() -> service.registerEntry(1L, 5, DATE, null))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The product is inactive");
    }

    @Test
    void movementsRejectAFutureDate() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerEntry(1L, 5, DATE.plusDays(1), null))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The journal date cannot be in the future");
    }

    @Test
    void movementsRejectAMissingDate() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerEntry(1L, 5, null, null))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The journal date is required");
    }

    // ---------- entries ----------

    @Test
    void entryGoesToTheOfficeWithATrimmedNote() {
        givenUsableProduct();
        givenSavingEchoes();

        StockMovement entry = service.registerEntry(1L, 30, DATE, "  Invoice 12  ");

        assertThat(entry.getLocation()).isEqualTo(OFFICE);
        assertThat(entry.getMovementType()).isEqualTo(ENTRY);
        assertThat(entry.getQuantity()).isEqualTo(30);
        assertThat(entry.getNote()).isEqualTo("Invoice 12");
    }

    @Test
    void aBlankNoteIsStoredAsNull() {
        givenUsableProduct();
        givenSavingEchoes();

        assertThat(service.registerEntry(1L, 30, DATE, "   ").getNote()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"0", "-3"})
    void entryRejectsAQuantityThatIsNotPositive(int quantity) {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerEntry(1L, quantity, DATE, null))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The entry quantity must be greater than zero");
    }

    @Test
    void entryRejectsATooLongNote() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerEntry(1L, 5, DATE, "x".repeat(201)))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The note cannot exceed 200 characters");
    }

    // ---------- transfers ----------

    @Test
    void transferTakesFromTheOfficeAndGivesToTheCounterTogether() {
        givenUsableProduct();
        givenSavingEchoes();
        when(movementRepository.sumQuantityUpTo(1L, OFFICE, DATE)).thenReturn(10);

        service.registerTransfer(1L, 10, DATE, "Restock");

        List<StockMovement> saved = savedMovements();
        assertThat(saved).extracting(StockMovement::getLocation).containsExactly(OFFICE, COUNTER);
        assertThat(saved).extracting(StockMovement::getQuantity).containsExactly(-10, 10);
        assertThat(saved).allSatisfy(movement -> {
            assertThat(movement.getMovementType()).isEqualTo(TRANSFER);
            assertThat(movement.getNote()).isEqualTo("Restock");
        });
    }

    @Test
    void transferRejectsMoreUnitsThanTheOfficeHas() {
        givenUsableProduct();
        when(movementRepository.sumQuantityUpTo(1L, OFFICE, DATE)).thenReturn(9);

        assertThatThrownBy(() -> service.registerTransfer(1L, 10, DATE, null))
                .isInstanceOf(StockConflictException.class)
                .hasMessage("The office does not have enough units for the transfer");
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void transferRejectsAQuantityThatIsNotPositive() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerTransfer(1L, 0, DATE, null))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The transfer quantity must be greater than zero");
    }

    // ---------- adjustments ----------

    @Test
    void aPositiveAdjustmentNeedsNoStockCheck() {
        givenUsableProduct();
        givenSavingEchoes();

        StockMovement adjustment = service.registerAdjustment(1L, COUNTER, 4, DATE, "Found in the shelf");

        assertThat(adjustment.getMovementType()).isEqualTo(ADJUSTMENT);
        assertThat(adjustment.getLocation()).isEqualTo(COUNTER);
        assertThat(adjustment.getQuantity()).isEqualTo(4);
        assertThat(adjustment.getNote()).isEqualTo("Found in the shelf");
        verify(movementRepository, never()).sumQuantityUpTo(any(), any(), any());
    }

    @Test
    void aNegativeAdjustmentCanEmptyTheLocation() {
        givenUsableProduct();
        givenSavingEchoes();
        when(movementRepository.sumQuantityUpTo(1L, COUNTER, DATE)).thenReturn(5);

        assertThat(service.registerAdjustment(1L, COUNTER, -5, DATE, "Damaged").getQuantity()).isEqualTo(-5);
    }

    @Test
    void aNegativeAdjustmentCannotLeaveTheStockBelowZero() {
        givenUsableProduct();
        when(movementRepository.sumQuantityUpTo(1L, COUNTER, DATE)).thenReturn(4);

        assertThatThrownBy(() -> service.registerAdjustment(1L, COUNTER, -5, DATE, "Damaged"))
                .isInstanceOf(StockConflictException.class)
                .hasMessage("The adjustment would leave the stock below zero");
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void adjustmentRejectsZero() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerAdjustment(1L, COUNTER, 0, DATE, "Why"))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The adjustment cannot be zero");
    }

    @Test
    void adjustmentRequiresAReason() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerAdjustment(1L, COUNTER, 2, DATE, "   "))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The reason of the adjustment is required");
    }

    @Test
    void adjustmentRequiresALocation() {
        givenUsableProduct();

        assertThatThrownBy(() -> service.registerAdjustment(1L, null, 2, DATE, "Reason"))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("The location is required");
    }

    // ---------- sales sync ----------

    @Test
    void syncReplacesTheSalesOfTheDateWithTheSoldUnits() {
        Product lubricant = product(1L, "LUB-1", ProductCategory.LUBRICANTE, true);
        Product additive = product(2L, "ADT-9", ProductCategory.ADITIVO, true);
        Product notSold = product(3L, "ZERO", ProductCategory.OTRO, true);
        when(productRepository.findAll(null, null)).thenReturn(List.of(lubricant, additive, notSold));
        when(salesSummaryPort.findByJournalDate(DATE)).thenReturn(Optional.of(new SalesSummary(DATE, Map.of(),
                Map.of("LUB-1", 3, "ADT-9", 2, "ZERO", 0, "XYZ", 4))));

        SalesSyncResult result = service.syncSales(DATE);

        verify(movementRepository).replaceSales(org.mockito.ArgumentMatchers.eq(DATE), movementsCaptor.capture());
        List<StockMovement> sales = movementsCaptor.getValue();
        assertThat(sales).extracting(StockMovement::getProductId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(sales).extracting(StockMovement::getQuantity).containsExactlyInAnyOrder(-3, -2);
        assertThat(sales).allSatisfy(movement -> {
            assertThat(movement.getLocation()).isEqualTo(COUNTER);
            assertThat(movement.getMovementType()).isEqualTo(SALE);
            assertThat(movement.getJournalDate()).isEqualTo(DATE);
        });
        assertThat(result.getSyncedProducts()).isEqualTo(2);
        assertThat(result.getUnmatchedCodes()).containsExactly("XYZ");
    }

    @Test
    void syncRecordsTheSalesOfAnInactiveProduct() {
        when(productRepository.findAll(null, null))
                .thenReturn(List.of(product(1L, "OLD-1", ProductCategory.OTRO, false)));
        when(salesSummaryPort.findByJournalDate(DATE))
                .thenReturn(Optional.of(new SalesSummary(DATE, Map.of(), Map.of("OLD-1", 2))));

        assertThat(service.syncSales(DATE).getSyncedProducts()).isEqualTo(1);
    }

    @Test
    void syncWithoutProductSalesClearsThePreviousOnes() {
        when(productRepository.findAll(null, null)).thenReturn(List.of());
        when(salesSummaryPort.findByJournalDate(DATE))
                .thenReturn(Optional.of(new SalesSummary(DATE, Map.of(), Map.of())));

        SalesSyncResult result = service.syncSales(DATE);

        verify(movementRepository).replaceSales(org.mockito.ArgumentMatchers.eq(DATE), movementsCaptor.capture());
        assertThat(movementsCaptor.getValue()).isEmpty();
        assertThat(result.getSyncedProducts()).isZero();
        assertThat(result.getUnmatchedCodes()).isEmpty();
    }

    @Test
    void syncFailsWhenThereAreNoSalesForTheDate() {
        when(salesSummaryPort.findByJournalDate(DATE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncSales(DATE)).isInstanceOf(NoSalesDataException.class);
        verify(movementRepository, never()).replaceSales(any(), anyList());
    }

    @Test
    void syncRejectsAFutureDate() {
        assertThatThrownBy(() -> service.syncSales(DATE.plusDays(1)))
                .isInstanceOf(InvalidStockMovementException.class);
    }

    // ---------- day report ----------

    @Test
    void closingStockAddsTheMovementsOfTheDayToTheOpening() {
        givenCatalog(List.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, true)),
                List.of(new StockBalance(1L, OFFICE, 10), new StockBalance(1L, COUNTER, 4)),
                List.of(movement(1L, DATE, OFFICE, ENTRY, 20),
                        movement(1L, DATE, OFFICE, TRANSFER, -6), movement(1L, DATE, COUNTER, TRANSFER, 6),
                        movement(1L, DATE, COUNTER, ADJUSTMENT, -1), movement(1L, DATE, COUNTER, SALE, -3)));

        ProductStock stock = onlyStock(null);

        LocationStock office = stock.getOffice();
        assertThat(office.getOpening()).isEqualTo(10);
        assertThat(office.getEntries()).isEqualTo(20);
        assertThat(office.getTransfers()).isEqualTo(-6);
        assertThat(office.getClosing()).isEqualTo(24);
        LocationStock counter = stock.getCounter();
        assertThat(counter.getOpening()).isEqualTo(4);
        assertThat(counter.getTransfers()).isEqualTo(6);
        assertThat(counter.getAdjustments()).isEqualTo(-1);
        assertThat(counter.getSold()).isEqualTo(3);
        assertThat(counter.getClosing()).isEqualTo(6);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.OK);
        assertThat(stock.getProductCode()).isEqualTo("LUB-1");
    }

    @Test
    void theInitialStockOfTheDayIsShownApartFromTheOpening() {
        givenCatalog(List.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, true)), List.of(),
                List.of(movement(1L, DATE, OFFICE, INITIAL, 12), movement(1L, DATE, COUNTER, INITIAL, 5)));

        ProductStock stock = onlyStock(null);

        assertThat(stock.getOffice().getOpening()).isZero();
        assertThat(stock.getOffice().getInitial()).isEqualTo(12);
        assertThat(stock.getOffice().getClosing()).isEqualTo(12);
        assertThat(stock.getCounter().getInitial()).isEqualTo(5);
        assertThat(stock.getCounter().getClosing()).isEqualTo(5);
    }

    @Test
    void sellingMoreThanTheCounterHasIsMarkedAsNegativeStockNotRejected() {
        givenCatalog(List.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, true)),
                List.of(new StockBalance(1L, COUNTER, 1)),
                List.of(movement(1L, DATE, COUNTER, SALE, -3)));

        ProductStock stock = onlyStock(null);

        assertThat(stock.getCounter().getClosing()).isEqualTo(-2);
        assertThat(stock.getStatus()).isEqualTo(StockStatus.NEGATIVE_STOCK);
    }

    @Test
    void anActiveProductWithoutMovementsAppearsWithZeros() {
        givenCatalog(List.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, true)), List.of(), List.of());

        ProductStock stock = onlyStock(null);

        assertThat(stock.getOffice().getClosing()).isZero();
        assertThat(stock.getCounter().getClosing()).isZero();
        assertThat(stock.getStatus()).isEqualTo(StockStatus.OK);
    }

    @Test
    void anInactiveProductAppearsOnlyIfItStillHasStock() {
        givenCatalog(List.of(product(1L, "OLD-1", ProductCategory.OTRO, false),
                        product(2L, "OLD-2", ProductCategory.OTRO, false)),
                List.of(new StockBalance(2L, OFFICE, 7)), List.of());

        assertThat(onlyStock(null).getProductCode()).isEqualTo("OLD-2");
    }

    @Test
    void theCategoryFilterKeepsOnlyThatCategory() {
        givenCatalog(List.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, true),
                product(2L, "ADT-1", ProductCategory.ADITIVO, true)), List.of(), List.of());

        assertThat(onlyStock(ProductCategory.ADITIVO).getProductCode()).isEqualTo("ADT-1");
    }

    // ---------- helpers ----------

    private void givenUsableProduct() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product(1L, "LUB-1", ProductCategory.LUBRICANTE, true)));
    }

    private void givenSavingEchoes() {
        when(movementRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private List<StockMovement> savedMovements() {
        verify(movementRepository).saveAll(movementsCaptor.capture());
        return movementsCaptor.getValue();
    }

    private void givenCatalog(List<Product> products, List<StockBalance> balances, List<StockMovement> movements) {
        when(productRepository.findAll(null, null)).thenReturn(products);
        when(movementRepository.findBalancesBefore(DATE)).thenReturn(balances);
        when(movementRepository.findByJournalDate(DATE)).thenReturn(movements);
    }

    private ProductStock onlyStock(ProductCategory category) {
        List<ProductStock> stocks = service.findByJournalDate(DATE, category);
        assertThat(stocks).hasSize(1);
        return stocks.get(0);
    }
}