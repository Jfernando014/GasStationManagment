package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.TankTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.FuelReceptionPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.TankPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reception;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "server.port=0")
@Import({FuelReceptionRepositoryAdapter.class, FuelReceptionPersistenceMapper.class,
        TankRepositoryAdapter.class, TankPersistenceMapper.class})
@Testcontainers
class FuelReceptionRepositoryAdapterTest {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_5 = LocalDate.of(2026, 8, 5);
    private static final LocalDate AUG_6 = LocalDate.of(2026, 8, 6);
    private static final LocalDate AUG_10 = LocalDate.of(2026, 8, 10);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private FuelReceptionRepositoryAdapter adapter;

    @Autowired
    private TankRepositoryAdapter tankRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long tank1Id;
    private Long tank2Id;
    private FuelReception aug10Tank1;

    @BeforeEach
    void setUp() {
        // fuel_reception.tank_id tiene FK contra tank(id): los tanques deben existir antes.
        tank1Id = tankRepository.save(TankTestData.tank(null, "T1C1", "Tanque 1")).getId();
        tank2Id = tankRepository.save(TankTestData.tank(null, "T2C1", "Tanque 2")).getId();
        entityManager.flush();

        adapter.save(reception(null, tank1Id, AUG_1, "100"));
        adapter.save(reception(null, tank1Id, AUG_5, "200"));
        aug10Tank1 = adapter.save(reception(null, tank1Id, AUG_10, "300"));
        adapter.save(reception(null, tank2Id, AUG_6, "50"));
        entityManager.flush();
        entityManager.clear();
    }

    // ---------- save / findById ----------

    @Test
    void savePersistsAllTheFields() {
        Tank tank = tankRepository.save(TankTestData.tank(null, "T3C1", "Tanque 3"));
        FuelReception saved = adapter.save(reception(null, tank.getId(), AUG_5, "3970.50"));
        entityManager.flush();
        entityManager.clear();

        FuelReception found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getTankId()).isEqualTo(tank.getId());
        assertThat(found.getReceptionDate()).isEqualTo(AUG_5);
        assertThat(found.getGallons()).isEqualByComparingTo("3970.50");
        assertThat(found.getSupplier()).isEqualTo("Terpel");
        assertThat(found.getCreatedAt()).isEqualTo(AUG_5.atTime(8, 0));
    }

    @Test
    void savingAnExistingReceptionUpdatesIt() {
        FuelReception found = adapter.findById(aug10Tank1.getId()).orElseThrow();
        found.setGallons(new java.math.BigDecimal("999"));

        adapter.save(found);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findById(aug10Tank1.getId()).get().getGallons()).isEqualByComparingTo("999");
    }

    @Test
    void findByIdReturnsEmptyWhenTheReceptionDoesNotExist() {
        assertThat(adapter.findById(9999L)).isEmpty();
    }

    // ---------- search ----------

    @Test
    void searchWithoutFiltersReturnsEverythingMostRecentFirst() {
        assertThat(adapter.search(null, null, null)).extracting(FuelReception::getReceptionDate)
                .containsExactly(AUG_10, AUG_6, AUG_5, AUG_1);
    }

    @Test
    void searchFiltersByTank() {
        assertThat(adapter.search(tank1Id, null, null)).extracting(FuelReception::getReceptionDate)
                .containsExactly(AUG_10, AUG_5, AUG_1);
    }

    @Test
    void searchFiltersByDateRangeIncludingBothLimits() {
        assertThat(adapter.search(null, AUG_5, AUG_6)).extracting(FuelReception::getReceptionDate)
                .containsExactly(AUG_6, AUG_5);
    }

    @Test
    void searchFiltersByOnlyOneLimit() {
        assertThat(adapter.search(null, AUG_6, null)).hasSize(2);
        assertThat(adapter.search(null, null, AUG_5)).hasSize(2);
    }

    @Test
    void searchCombinesTankAndDateRange() {
        assertThat(adapter.search(tank1Id, AUG_5, null)).extracting(FuelReception::getReceptionDate)
                .containsExactly(AUG_10, AUG_5);
    }

    @Test
    void searchReturnsAnEmptyListWhenNothingMatches() {
        assertThat(adapter.search(9999L, null, null)).isEmpty();
    }

    @Test
    void receptionsOfTheSameDateComeNewestFirst() {
        Tank tank = tankRepository.save(TankTestData.tank(null, "T4C1", "Tanque 4"));
        FuelReception first = adapter.save(reception(null, tank.getId(), AUG_5, "10"));
        FuelReception second = adapter.save(reception(null, tank.getId(), AUG_5, "20"));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.search(tank.getId(), null, null)).extracting(FuelReception::getId)
                .containsExactly(second.getId(), first.getId());
    }

    // ---------- sumGallonsAfter ----------

    @Test
    void sumGallonsAfterAddsTheReceptionsOfLaterDatesOfTheTank() {
        assertThat(adapter.sumGallonsAfter(tank1Id, AUG_1, null)).isEqualByComparingTo("500");
    }

    @Test
    void sumGallonsAfterIsStrictlyAfterTheDate() {
        assertThat(adapter.sumGallonsAfter(tank1Id, AUG_5, null)).isEqualByComparingTo("300");
    }

    @Test
    void sumGallonsAfterIgnoresTheExcludedReception() {
        assertThat(adapter.sumGallonsAfter(tank1Id, AUG_1, aug10Tank1.getId())).isEqualByComparingTo("200");
    }

    @Test
    void sumGallonsAfterIsZeroWhenThereAreNoLaterReceptions() {
        assertThat(adapter.sumGallonsAfter(tank1Id, AUG_10, null)).isEqualByComparingTo("0");
        assertThat(adapter.sumGallonsAfter(9999L, AUG_1, null)).isEqualByComparingTo("0");
    }
}