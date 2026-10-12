package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.GaugeTablePersistenceMapper;
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

import java.util.ArrayList;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.curve;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.point;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.table;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({GaugeTableRepositoryAdapter.class, GaugeTablePersistenceMapper.class})
@Testcontainers
class GaugeTableRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GaugeTableRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savePersistsTheTableAndItsPointsOrderedByHeight() {
        GaugeTable saved = adapter.save(curve(null, "Tanques 1 y 2"));
        GaugeTable found = reload(saved.getId());

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Tanques 1 y 2");
        assertThat(found.getPoints()).extracting(p -> p.getHeightCm().doubleValue())
                .containsExactly(1.0, 5.0, 6.0, 54.0, 55.0, 56.0, 238.0);
        assertThat(found.getPoints().get(4).getVolumeGallons()).isEqualByComparingTo("559");
    }

    @Test
    void savePersistsAHeightWithOneDecimal() {
        GaugeTable saved = adapter.save(table(null, "Curva", point("55.5", "566.5"), point("56", "574")));

        assertThat(reload(saved.getId()).getPoints().get(0).getHeightCm()).isEqualByComparingTo("55.5");
    }

    @Test
    void saveReplacesThePointsEvenWhenTheHeightsRepeat() {
        // Mismo conjunto de alturas con otros volúmenes: borrar antes de insertar evita chocar con el UNIQUE
        GaugeTable saved = adapter.save(table(null, "Curva", point("1", "1"), point("2", "4"), point("3", "7")));
        saved.setPoints(new ArrayList<>(List.of(point("1", "2"), point("2", "5"), point("3", "8"))));

        adapter.save(saved);
        GaugeTable found = reload(saved.getId());

        assertThat(found.getPoints()).hasSize(3);
        assertThat(found.getPoints()).extracting(GaugePoint::getVolumeGallons)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("2"), new java.math.BigDecimal("5"), new java.math.BigDecimal("8"));
    }

    @Test
    void saveRemovesThePointsThatAreNoLongerInTheTable() {
        GaugeTable saved = adapter.save(curve(null, "Curva"));
        saved.setPoints(new ArrayList<>(List.of(point("1", "1"), point("2", "4"))));

        adapter.save(saved);

        assertThat(reload(saved.getId()).getPoints()).hasSize(2);
    }

    @Test
    void saveUpdatesTheNameOfAnExistingTable() {
        GaugeTable saved = adapter.save(curve(null, "Anterior"));
        saved.setName("Nuevo");

        adapter.save(saved);

        assertThat(reload(saved.getId()).getName()).isEqualTo("Nuevo");
        assertThat(adapter.findAllOrderByName()).hasSize(1);
    }

    @Test
    void saveRejectsADuplicatedName() {
        adapter.save(curve(null, "Misma"));

        assertThatThrownBy(() -> adapter.save(curve(null, "Misma")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByIdReturnsEmptyWhenTheTableDoesNotExist() {
        assertThat(adapter.findById(999L)).isEmpty();
    }

    @Test
    void existsByIdAndByNameReflectTheStoredTables() {
        GaugeTable saved = adapter.save(curve(null, "Tanques 1 y 2"));

        assertThat(adapter.existsById(saved.getId())).isTrue();
        assertThat(adapter.existsById(999L)).isFalse();
        assertThat(adapter.existsByName("Tanques 1 y 2")).isTrue();
        assertThat(adapter.existsByName("Otra")).isFalse();
    }

    @Test
    void existsByNameAndIdNotIgnoresTheTableItself() {
        GaugeTable first = adapter.save(curve(null, "Tanques 1 y 2"));
        GaugeTable second = adapter.save(curve(null, "Tanques 3 y 4"));

        assertThat(adapter.existsByNameAndIdNot("Tanques 1 y 2", first.getId())).isFalse();
        assertThat(adapter.existsByNameAndIdNot("Tanques 1 y 2", second.getId())).isTrue();
    }

    @Test
    void findAllOrderByNameReturnsTheTablesWithTheirOwnPoints() {
        adapter.save(table(null, "Tanques 3 y 4", point("1", "1"), point("2", "3")));
        adapter.save(curve(null, "Tanques 1 y 2"));
        entityManager.flush();
        entityManager.clear();

        List<GaugeTable> tables = adapter.findAllOrderByName();

        assertThat(tables).extracting(GaugeTable::getName).containsExactly("Tanques 1 y 2", "Tanques 3 y 4");
        assertThat(tables.get(0).getPoints()).hasSize(7);
        assertThat(tables.get(1).getPoints()).hasSize(2);
    }

    @Test
    void findAllOrderByNameReturnsAnEmptyListWhenThereAreNoTables() {
        assertThat(adapter.findAllOrderByName()).isEmpty();
    }

    /** Fuerza ir y volver a la base de datos para no leer instancias ya cargadas en memoria. */
    private GaugeTable reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return adapter.findById(id).orElseThrow();
    }
}