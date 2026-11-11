package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.InspectionPersistenceMapper;
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

import static com.edu.unicauca.gasstation.backend.inventory.InspectionTestData.inspection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "server.port=0")
@Import({InspectionRepositoryAdapter.class, InspectionPersistenceMapper.class})
@Testcontainers
class InspectionRepositoryAdapterTest {

    private static final LocalDate FEB_18 = LocalDate.of(2026, 2, 18);
    private static final LocalDate MAR_15 = LocalDate.of(2026, 3, 15);
    private static final LocalDate APR_10 = LocalDate.of(2026, 4, 10);
    private static final LocalDate MAY_12 = LocalDate.of(2026, 5, 12);
    private static final LocalDate SEP_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate OCT_10 = LocalDate.of(2026, 10, 10);
    private static final LocalDate FEB_18_2027 = LocalDate.of(2027, 2, 18);
    private static final LocalDate MAY_12_2027 = LocalDate.of(2027, 5, 12);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InspectionRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private Inspection onacMarch;

    @BeforeEach
    void setUp() {
        onacMarch = adapter.save(inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                MAR_15, SEP_15));
        adapter.save(inspection(null, "CAL-ONAC-2026-09", InspectionCategory.ONAC, APR_10, OCT_10));
        adapter.save(inspection(null, "HERM-TK-2026-01", InspectionCategory.HERMETICIDAD,
                FEB_18, FEB_18_2027));
        adapter.save(inspection(null, "AMB-CRC-2026-01", InspectionCategory.AMBIENTAL,
                MAY_12, MAY_12_2027));
        entityManager.flush();
        entityManager.clear();
    }

    // ---------- save / findById ----------

    @Test
    void savePersistsAllTheFields() {
        Inspection saved = adapter.save(inspection(null, "BOMB-RED-2026-02", InspectionCategory.BOMBAS,
                MAR_15, SEP_15));
        entityManager.flush();
        entityManager.clear();

        Inspection found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getCode()).isEqualTo("BOMB-RED-2026-02");
        assertThat(found.getCategory()).isEqualTo(InspectionCategory.BOMBAS);
        assertThat(found.getTarget()).isEqualTo("Dispensador 1 y 2 (Mangueras 1 a 6)");
        assertThat(found.getDateExecuted()).isEqualTo(MAR_15);
        assertThat(found.getDateDue()).isEqualTo(SEP_15);
        assertThat(found.getCertifier()).isEqualTo("Metrología del Cauca SAS (ONAC 19-LAB-042)");
        assertThat(found.getToleranceMeasured()).isEqualTo("+3,8 mL (Límite ±15 mL)");
        assertThat(found.getCertDoc()).isEqualTo("CERT-ONAC-0812.pdf");
        assertThat(found.getCreatedAt()).isEqualTo(MAR_15.atTime(8, 0));
    }

    @Test
    void savingAnExistingInspectionUpdatesIt() {
        Inspection found = adapter.findById(onacMarch.getId()).orElseThrow();
        found.setToleranceMeasured("+4,2 mL (Límite ±15 mL)");

        adapter.save(found);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findById(onacMarch.getId()).get().getToleranceMeasured())
                .isEqualTo("+4,2 mL (Límite ±15 mL)");
    }

    @Test
    void findByIdReturnsEmptyWhenTheInspectionDoesNotExist() {
        assertThat(adapter.findById(9999L)).isEmpty();
    }

    // ---------- code ----------

    @Test
    void findByCodeReturnsTheInspection() {
        assertThat(adapter.findByCode("CAL-ONAC-2026-08")).get()
                .extracting(Inspection::getId).isEqualTo(onacMarch.getId());
        assertThat(adapter.findByCode("00000")).isEmpty();
    }

    @Test
    void existsByCodeChecksTheWholeLog() {
        assertThat(adapter.existsByCode("CAL-ONAC-2026-08")).isTrue();
        assertThat(adapter.existsByCode("00000")).isFalse();
    }

    @Test
    void existsByCodeAndIdNotIgnoresTheInspectionItself() {
        assertThat(adapter.existsByCodeAndIdNot("CAL-ONAC-2026-08", onacMarch.getId())).isFalse();
        assertThat(adapter.existsByCodeAndIdNot("CAL-ONAC-2026-08", 9999L)).isTrue();
    }

    @Test
    void theCodeIsUnique() {
        Inspection duplicate = inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                MAR_15, SEP_15);

        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- search ----------

    @Test
    void searchWithoutFiltersReturnsEverythingWithTheNearestDueDateFirst() {
        assertThat(adapter.search(null, null, null)).extracting(Inspection::getCode)
                .containsExactly("CAL-ONAC-2026-08", "CAL-ONAC-2026-09",
                        "HERM-TK-2026-01", "AMB-CRC-2026-01");
    }

    @Test
    void searchFiltersByCategory() {
        assertThat(adapter.search(InspectionCategory.ONAC, null, null))
                .extracting(Inspection::getCode)
                .containsExactly("CAL-ONAC-2026-08", "CAL-ONAC-2026-09");
    }

    @Test
    void searchFiltersByExecutionDateRangeIncludingBothLimits() {
        assertThat(adapter.search(null, MAR_15, APR_10)).extracting(Inspection::getCode)
                .containsExactly("CAL-ONAC-2026-08", "CAL-ONAC-2026-09");
    }

    @Test
    void searchFiltersByOnlyOneLimit() {
        assertThat(adapter.search(null, APR_10, null)).hasSize(2);
        assertThat(adapter.search(null, null, APR_10)).hasSize(3);
    }

    @Test
    void searchCombinesCategoryAndDateRange() {
        assertThat(adapter.search(InspectionCategory.ONAC, APR_10, null))
                .extracting(Inspection::getCode)
                .containsExactly("CAL-ONAC-2026-09");
    }

    @Test
    void searchReturnsAnEmptyListWhenNothingMatches() {
        assertThat(adapter.search(InspectionCategory.BOMBAS, null, null)).isEmpty();
        assertThat(adapter.search(null, LocalDate.of(2030, 1, 1), null)).isEmpty();
    }

    @Test
    void inspectionsOfTheSameDueDateComeNewestFirst() {
        Inspection first = adapter.save(inspection(null, "CAL-ONAC-2026-10", InspectionCategory.ONAC,
                MAY_12, SEP_15));
        Inspection second = adapter.save(inspection(null, "CAL-ONAC-2026-11", InspectionCategory.ONAC,
                MAY_12, SEP_15));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.search(InspectionCategory.ONAC, MAY_12, MAY_12))
                .extracting(Inspection::getId)
                .containsExactly(second.getId(), first.getId());
    }
}
