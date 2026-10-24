package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.TankMeasurementPersistenceMapper;
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

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TankMeasurementRepositoryAdapter.class, TankMeasurementPersistenceMapper.class})
@Testcontainers
class TankMeasurementRepositoryAdapterTest {

    private static final LocalDate D1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate D3 = LocalDate.of(2026, 8, 3);
    private static final LocalDate D5 = LocalDate.of(2026, 8, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TankMeasurementRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savePersistsTheMeasurementWithItsReadingsOrderedByTank() {
        TankMeasurement saved = adapter.save(measurement(null, D5, reading(2L, "20.5", "200.25"), reading(1L, "10", "100")));

        TankMeasurement found = reload(D5);

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getMeasuredAt()).isEqualTo(D5.atTime(9, 0));
        assertThat(found.getReadings()).extracting(r -> r.getTankId()).containsExactly(1L, 2L);
        assertThat(found.readingOf(2L).get().getHeightCm()).isEqualByComparingTo("20.5");
        assertThat(found.readingOf(2L).get().getVolumeGallons()).isEqualByComparingTo("200.25");
    }

    @Test
    void aSecondMeasurementForTheSameDateViolatesTheUniqueDate() {
        adapter.save(measurement(null, D5, reading(1L, "10", "100")));

        assertThatThrownBy(() -> adapter.save(measurement(null, D5, reading(1L, "11", "110"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingAnExistingMeasurementReplacesItsReadingsEvenForTheSameTank() {
        TankMeasurement saved = adapter.save(measurement(null, D5, reading(1L, "10", "100"), reading(2L, "20", "200")));
        saved.setReadings(List.of(reading(1L, "15", "150")));

        adapter.save(saved);
        TankMeasurement found = reload(D5);

        assertThat(found.getReadings()).hasSize(1);
        assertThat(found.readingOf(1L).get().getHeightCm()).isEqualByComparingTo("15");
        assertThat(found.readingOf(2L)).isEmpty();
    }

    @Test
    void findByJournalDateReturnsEmptyWhenThereIsNoMeasurement() {
        assertThat(adapter.findByJournalDate(D5)).isEmpty();
    }

    @Test
    void existsByJournalDateReflectsTheStoredMeasurements() {
        adapter.save(measurement(null, D5, reading(1L, "10", "100")));

        assertThat(adapter.existsByJournalDate(D5)).isTrue();
        assertThat(adapter.existsByJournalDate(D3)).isFalse();
    }

    @Test
    void findLatestBeforeReturnsTheClosestEarlierMeasurement() {
        adapter.save(measurement(null, D1, reading(1L, "10", "100")));
        adapter.save(measurement(null, D3, reading(1L, "11", "110")));
        adapter.save(measurement(null, D5, reading(1L, "12", "120")));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findLatestBefore(D5).get().getJournalDate()).isEqualTo(D3);
        assertThat(adapter.findLatestBefore(LocalDate.of(2026, 8, 10)).get().getJournalDate()).isEqualTo(D5);
        assertThat(adapter.findLatestBefore(D1)).isEmpty();
        assertThat(adapter.findLatestBefore(D5).get().getReadings()).hasSize(1);
    }

    @Test
    void findLatestContainingTankSkipsMeasurementsThatDoNotIncludeTheTank() {
        adapter.save(measurement(null, D1, reading(1L, "10", "100"), reading(2L, "20", "200")));
        adapter.save(measurement(null, D3, reading(1L, "11", "110")));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findLatestContainingTank(1L).get().getJournalDate()).isEqualTo(D3);
        // el tanque 2 no se midió el 03/08: su última medición es la del 01/08
        assertThat(adapter.findLatestContainingTank(2L).get().getJournalDate()).isEqualTo(D1);
        assertThat(adapter.findLatestContainingTank(2L).get().readingOf(2L).get().getVolumeGallons())
                .isEqualByComparingTo("200");
        assertThat(adapter.findLatestContainingTank(3L)).isEmpty();
    }

    /** Fuerza ir y volver a la base de datos para no leer instancias ya cargadas en memoria. */
    private TankMeasurement reload(LocalDate journalDate) {
        entityManager.flush();
        entityManager.clear();
        return adapter.findByJournalDate(journalDate).orElseThrow();
    }
}