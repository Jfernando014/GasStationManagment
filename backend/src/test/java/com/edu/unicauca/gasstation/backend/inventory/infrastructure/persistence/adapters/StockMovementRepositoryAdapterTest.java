package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockBalance;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.ProductPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.StockMovementPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "server.port=0")
@Import({StockMovementRepositoryAdapter.class, StockMovementPersistenceMapper.class,
        ProductRepositoryAdapter.class, ProductPersistenceMapper.class})
@Testcontainers
class StockMovementRepositoryAdapterTest {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_4 = LocalDate.of(2026, 8, 4);
    private static final LocalDate AUG_5 = LocalDate.of(2026, 8, 5);
    private static final LocalDate AUG_6 = LocalDate.of(2026, 8, 6);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StockMovementRepositoryAdapter adapter;

    @Autowired
    private ProductRepositoryAdapter productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long productA;
    private Long productB;

    @BeforeEach
    void setUp() {
        // The migration has a foreign key to the product table: the products must really exist
        productA = productRepository.save(product(null, "LUB-A", ProductCategory.LUBRICANTE, true)).getId();
        productB = productRepository.save(product(null, "ADT-B", ProductCategory.ADITIVO, true)).getId();
    }

    // ---------- save / find ----------

    @Test
    void saveAllPersistsAllTheFields() {
        StockMovement entry = movement(productA, AUG_5, OFFICE, ENTRY, 30);
        entry.setNote("Invoice 12");

        List<StockMovement> saved = adapter.saveAll(List.of(entry));
        flushAndClear();

        StockMovement found = adapter.findByJournalDate(AUG_5).get(0);
        assertThat(saved.get(0).getId()).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.get(0).getId());
        assertThat(found.getProductId()).isEqualTo(productA);
        assertThat(found.getJournalDate()).isEqualTo(AUG_5);
        assertThat(found.getLocation()).isEqualTo(OFFICE);
        assertThat(found.getMovementType()).isEqualTo(ENTRY);
        assertThat(found.getQuantity()).isEqualTo(30);
        assertThat(found.getNote()).isEqualTo("Invoice 12");
        assertThat(found.getCreatedAt()).isEqualTo(entry.getCreatedAt());
    }

    @Test
    void findByJournalDateReturnsOnlyThatDateInRegistrationOrder() {
        adapter.saveAll(List.of(movement(productA, AUG_5, OFFICE, ENTRY, 1),
                movement(productA, AUG_6, OFFICE, ENTRY, 2),
                movement(productB, AUG_5, OFFICE, ENTRY, 3)));
        flushAndClear();

        assertThat(adapter.findByJournalDate(AUG_5)).extracting(StockMovement::getQuantity).containsExactly(1, 3);
        assertThat(adapter.findByJournalDate(AUG_4)).isEmpty();
    }

    // ---------- balances ----------

    @Test
    void sumQuantityUpToAddsTheMovementsOfTheProductAndLocationUpToTheDate() {
        givenLedgerOfProductA();

        assertThat(adapter.sumQuantityUpTo(productA, OFFICE, AUG_4)).isEqualTo(10);
        assertThat(adapter.sumQuantityUpTo(productA, OFFICE, AUG_5)).isEqualTo(15);
        assertThat(adapter.sumQuantityUpTo(productA, OFFICE, AUG_6)).isEqualTo(12);
        assertThat(adapter.sumQuantityUpTo(productA, COUNTER, AUG_6)).isEqualTo(5);
    }

    @Test
    void sumQuantityUpToIsZeroWithoutMovements() {
        givenLedgerOfProductA();

        assertThat(adapter.sumQuantityUpTo(productA, OFFICE, LocalDate.of(2026, 7, 31))).isZero();
        assertThat(adapter.sumQuantityUpTo(productB, OFFICE, AUG_6)).isZero();
    }

    @Test
    void findBalancesBeforeCountsOnlyTheMovementsBeforeTheDate() {
        givenLedgerOfProductA();

        List<StockBalance> balances = adapter.findBalancesBefore(AUG_6);

        assertThat(balances).hasSize(2);
        assertThat(balanceOf(balances, productA, OFFICE)).isEqualTo(15);
        assertThat(balanceOf(balances, productA, COUNTER)).isEqualTo(2);
        assertThat(adapter.findBalancesBefore(AUG_1)).isEmpty();
    }

    @Test
    void existsInitialIsPerProductAndLocation() {
        adapter.saveAll(List.of(movement(productA, AUG_1, OFFICE, INITIAL, 10)));
        flushAndClear();

        assertThat(adapter.existsInitial(productA, OFFICE)).isTrue();
        assertThat(adapter.existsInitial(productA, COUNTER)).isFalse();
        assertThat(adapter.existsInitial(productB, OFFICE)).isFalse();
    }

    // ---------- sales replacement ----------

    @Test
    void replaceSalesOnlyReplacesTheSalesOfThatDate() {
        adapter.saveAll(List.of(
                movement(productA, AUG_5, COUNTER, SALE, -3),
                movement(productA, AUG_5, OFFICE, ENTRY, 9),
                movement(productA, AUG_6, COUNTER, SALE, -1)));
        flushAndClear();

        adapter.replaceSales(AUG_5, List.of(movement(productB, AUG_5, COUNTER, SALE, -7)));
        flushAndClear();

        assertThat(adapter.findByJournalDate(AUG_5))
                .extracting(StockMovement::getProductId, StockMovement::getMovementType, StockMovement::getQuantity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(productA, ENTRY, 9),
                        org.assertj.core.groups.Tuple.tuple(productB, SALE, -7));
        assertThat(adapter.findByJournalDate(AUG_6)).hasSize(1);
    }

    @Test
    void replaceSalesWithAnEmptyListClearsThem() {
        adapter.saveAll(List.of(movement(productA, AUG_5, COUNTER, SALE, -3)));
        flushAndClear();

        adapter.replaceSales(AUG_5, List.of());
        flushAndClear();

        assertThat(adapter.findByJournalDate(AUG_5)).isEmpty();
    }

    // ---------- database rules ----------

    @Test
    void theDatabaseRejectsASecondInitialStockForTheSameProductAndLocation() {
        adapter.saveAll(List.of(movement(productA, AUG_1, OFFICE, INITIAL, 10)));
        flushAndClear();

        assertThatThrownBy(() -> adapter.saveAll(List.of(movement(productA, AUG_5, OFFICE, INITIAL, 3))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsTwoSalesOfTheSameProductOnTheSameDay() {
        adapter.saveAll(List.of(movement(productA, AUG_5, COUNTER, SALE, -3)));
        flushAndClear();

        assertThatThrownBy(() -> adapter.saveAll(List.of(movement(productA, AUG_5, COUNTER, SALE, -1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAnEntryThatIsNotPositive() {
        assertThatThrownBy(() -> adapter.saveAll(List.of(movement(productA, AUG_5, OFFICE, ENTRY, -1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsASaleThatIsNotNegative() {
        assertThatThrownBy(() -> adapter.saveAll(List.of(movement(productA, AUG_5, COUNTER, SALE, 2))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAProductThatDoesNotExist() {
        assertThatThrownBy(() -> adapter.saveAll(List.of(movement(9999L, AUG_5, OFFICE, ENTRY, 1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- helpers ----------

    /** Office: 10 on 01/08, +5 on 05/08, -3 on 06/08. Counter: 2 on 01/08, +3 on 06/08. */
    private void givenLedgerOfProductA() {
        adapter.saveAll(List.of(
                movement(productA, AUG_1, OFFICE, INITIAL, 10),
                movement(productA, AUG_1, COUNTER, INITIAL, 2),
                movement(productA, AUG_5, OFFICE, ENTRY, 5),
                movement(productA, AUG_6, OFFICE, TRANSFER, -3),
                movement(productA, AUG_6, COUNTER, TRANSFER, 3)));
        flushAndClear();
    }

    private int balanceOf(List<StockBalance> balances, Long productId, StockLocation location) {
        return balances.stream()
                .filter(balance -> balance.getProductId().equals(productId) && balance.getLocation() == location)
                .findFirst().orElseThrow().getQuantity();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}