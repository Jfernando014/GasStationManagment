package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.InventoryAlertPersistenceMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.alert;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.DIESEL;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.EXTRA;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MAX_PRO;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "server.port=0")
@Import({InventoryAlertRepositoryAdapter.class, InventoryAlertPersistenceMapper.class})
@Testcontainers
class InventoryAlertRepositoryAdapterTest {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_3 = LocalDate.of(2026, 8, 3);
    private static final LocalDate AUG_5 = LocalDate.of(2026, 8, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventoryAlertRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    // ---------- save / find ----------

    @Test
    void savePersistsAllTheFieldsAndAssignsAnId() {
        InventoryAlert attended = alert(null, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "1.25", AUG_5);

        InventoryAlert saved = adapter.save(attended);
        flushAndClear();

        InventoryAlert found = adapter.findById(saved.getId()).orElseThrow();
        assertThat(saved.getId()).isNotNull();
        assertThat(found.getAlertType()).isEqualTo(AlertType.AUTONOMY);
        assertThat(found.getFuelType()).isEqualTo(MOTOR);
        assertThat(found.getLevel()).isEqualTo(AutonomyLevel.CRITICAL);
        assertThat(found.getStatus()).isEqualTo(AlertStatus.ATTENDED);
        assertThat(found.getAutonomyDays()).isEqualByComparingTo("1.25");
        assertThat(found.getOpenedOn()).isEqualTo(AUG_5);
        assertThat(found.getCreatedAt()).isEqualTo(attended.getCreatedAt());
        assertThat(found.getReadAt()).isEqualTo(attended.getReadAt());
        assertThat(found.getAttendedAt()).isEqualTo(attended.getAttendedAt());
        assertThat(found.getAttendNote()).isEqualTo("Fuel ordered");
        assertThat(found.getResolvedAt()).isNull();
    }

    @Test
    void savingAnAlertWithAnIdUpdatesIt() {
        InventoryAlert saved = adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_5));
        flushAndClear();

        InventoryAlert loaded = adapter.findById(saved.getId()).orElseThrow();
        loaded.setLevel(AutonomyLevel.CRITICAL);
        loaded.setAutonomyDays(new BigDecimal("1.50"));
        adapter.save(loaded);
        flushAndClear();

        InventoryAlert updated = adapter.findById(saved.getId()).orElseThrow();
        assertThat(updated.getLevel()).isEqualTo(AutonomyLevel.CRITICAL);
        assertThat(updated.getAutonomyDays()).isEqualByComparingTo("1.5");
    }

    // ---------- open alert ----------

    @Test
    void findOpenIgnoresTheResolvedAlerts() {
        adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.RESOLVED, "4.00", AUG_1));
        InventoryAlert open = adapter.save(alert(null, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.00", AUG_5));
        flushAndClear();

        assertThat(adapter.findOpen(AlertType.AUTONOMY, MOTOR)).get()
                .extracting(InventoryAlert::getId).isEqualTo(open.getId());
        assertThat(adapter.findOpen(AlertType.AUTONOMY, DIESEL)).isEmpty();
    }

    @Test
    void afterResolvingAnAlertTheFuelCanHaveANewOne() {
        InventoryAlert first = adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_1));
        flushAndClear();
        InventoryAlert loaded = adapter.findById(first.getId()).orElseThrow();
        loaded.setStatus(AlertStatus.RESOLVED);
        loaded.setResolvedAt(AUG_3.atTime(10, 0));
        adapter.save(loaded);
        flushAndClear();

        InventoryAlert second = adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.50", AUG_5));

        assertThat(second.getId()).isNotEqualTo(first.getId());
    }

    // ---------- list and count ----------

    @Test
    void findByStatusesReturnsTheNewestOpenedFirstAndTheLatestIdOnATie() {
        adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_1));
        InventoryAlert diesel = adapter.save(alert(null, DIESEL, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.00", AUG_5));
        InventoryAlert maxPro = adapter.save(alert(null, MAX_PRO, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "3.00", AUG_5));
        adapter.save(alert(null, EXTRA, AutonomyLevel.WARNING, AlertStatus.RESOLVED, "4.00", AUG_3));
        flushAndClear();

        List<InventoryAlert> pending = adapter.findByStatuses(List.of(AlertStatus.ACTIVE, AlertStatus.READ));

        assertThat(pending).extracting(InventoryAlert::getId)
                .containsExactly(maxPro.getId(), diesel.getId(), pending.get(2).getId());
        assertThat(pending.get(2).getFuelType()).isEqualTo(MOTOR);
        assertThat(adapter.findByStatuses(List.of(AlertStatus.RESOLVED))).hasSize(1);
    }

    @Test
    void findByOpenedOnReturnsOnlyTheAlertsOpenedThatDayInIdOrder() {
        adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_1));
        InventoryAlert diesel = adapter.save(alert(null, DIESEL, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.00", AUG_5));
        InventoryAlert maxPro = adapter.save(alert(null, MAX_PRO, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "3.00", AUG_5));
        flushAndClear();

        List<InventoryAlert> opened = adapter.findByOpenedOn(AUG_5);

        assertThat(opened).extracting(InventoryAlert::getId)
                .containsExactly(diesel.getId(), maxPro.getId());
        assertThat(adapter.findByOpenedOn(AUG_3)).isEmpty();
    }

    @Test
    void countByStatusesCountsOnlyThoseStatuses() {
        adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_5));
        adapter.save(alert(null, DIESEL, AutonomyLevel.WARNING, AlertStatus.READ, "4.00", AUG_5));
        adapter.save(alert(null, EXTRA, AutonomyLevel.WARNING, AlertStatus.ATTENDED, "4.00", AUG_5));
        adapter.save(alert(null, MAX_PRO, AutonomyLevel.WARNING, AlertStatus.RESOLVED, "4.00", AUG_5));
        flushAndClear();

        assertThat(adapter.countByStatuses(List.of(AlertStatus.ACTIVE, AlertStatus.READ))).isEqualTo(2);
        assertThat(adapter.countByStatuses(List.of())).isZero();
    }

    // ---------- database rules ----------

    @Test
    void theDatabaseRejectsASecondOpenAlertOfTheSameFuel() {
        adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_1));
        flushAndClear();

        assertThatThrownBy(() ->
                adapter.save(alert(null, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "1.00", AUG_5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAnOkLevel() {
        assertThatThrownBy(() ->
                adapter.save(alert(null, MOTOR, AutonomyLevel.OK, AlertStatus.ACTIVE, "9.00", AUG_5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsNegativeDays() {
        assertThatThrownBy(() ->
                adapter.save(alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "-1.00", AUG_5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAnAttendedAlertWithoutAttendedTime() {
        InventoryAlert broken = alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ATTENDED, "4.00", AUG_5);
        broken.setAttendedAt(null);

        assertThatThrownBy(() -> adapter.save(broken)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAResolvedAlertWithoutResolvedTime() {
        InventoryAlert broken = alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.RESOLVED, "4.00", AUG_5);
        broken.setResolvedAt(null);

        assertThatThrownBy(() -> adapter.save(broken)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAnOpenAlertWithResolvedTime() {
        InventoryAlert broken = alert(null, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", AUG_5);
        broken.setResolvedAt(AUG_5.atTime(12, 0));

        assertThatThrownBy(() -> adapter.save(broken)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}