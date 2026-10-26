package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters.RoleRepositoryImpl;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters.ShiftAssignmentRepositoryImpl;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters.ShiftCodeRepositoryImpl;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers.ShiftCodePersistenceMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ShiftAssignmentRepositoryImpl.class,
        ShiftCodeRepositoryImpl.class, ShiftCodePersistenceMapper.class,
        RoleRepositoryImpl.class})
class ShiftAssignmentRepositoryTest {

    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BRUNO_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_2 = DAY_1.plusDays(1);
    private static final LocalDate DAY_3 = DAY_1.plusDays(2);
    private static final LocalDate DAY_4 = DAY_1.plusDays(3);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ShiftCode dia;
    private ShiftCode noche;
    private ShiftCode descanso;

    @BeforeEach
    void saveCatalog() {
        Role titular = roleRepository.save(new Role(null, "TITULAR", 1));
        dia = shiftCodeRepository.save(ShiftTestData.newShiftCode("DIA", titular, null));
        noche = shiftCodeRepository.save(ShiftTestData.newShiftCode("NOCHE", titular, null));
        descanso = shiftCodeRepository.save(ShiftTestData.newShiftCode("DESCANSO", null, null));
        clearContext();
    }

    @Test
    void saveGeneratesTheIdAndSavingItAgainUpdatesInsteadOfInserting() {
        ShiftAssignment saved = assignmentRepository.save(new ShiftAssignment(ANA_ID, DAY_1, dia, "Cubre a Bruno"));
        clearContext();

        ShiftAssignment existing = assignmentRepository.findByWorkerIdAndWorkDate(ANA_ID, DAY_1).orElseThrow();
        assertThat(existing.getId()).isEqualTo(saved.getId());
        assertThat(existing.getNote()).isEqualTo("Cubre a Bruno");

        existing.change(noche, "a".repeat(255));
        assignmentRepository.save(existing);
        clearContext();

        assertThat(assignmentRepository.findByWorkDateBetween(DAY_1, DAY_1)).singleElement().satisfies(row -> {
            assertThat(row.getId()).isEqualTo(saved.getId());
            assertThat(row.getShiftCode().getCode()).isEqualTo("NOCHE");
            assertThat(row.getNote()).hasSize(255);
        });
    }

    @Test
    void aWorkerHasASingleAssignmentPerDay() {
        assignmentRepository.save(new ShiftAssignment(ANA_ID, DAY_1, dia, null));
        assignmentRepository.save(new ShiftAssignment(ANA_ID, DAY_1, noche, null));

        // The query flushes the pending INSERTs; uk_shift_assignment_worker_date rejects the second one
        assertThatThrownBy(() -> assignmentRepository.findByWorkerIdAndWorkDate(ANA_ID, DAY_1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_shift_assignment_worker_date");
    }

    @Test
    void findByWorkDateBetweenIncludesBothEndsAndComesWithTheCodeAndItsRole() {
        assignmentRepository.saveAll(List.of(
                new ShiftAssignment(ANA_ID, DAY_1, dia, null),
                new ShiftAssignment(ANA_ID, DAY_2, noche, null),
                new ShiftAssignment(ANA_ID, DAY_3, descanso, null),
                new ShiftAssignment(ANA_ID, DAY_4, dia, null)));
        clearContext();

        List<ShiftAssignment> rows = assignmentRepository.findByWorkDateBetween(DAY_2, DAY_3);
        entityManager.clear();

        assertThat(rows).extracting(ShiftAssignment::getWorkDate).containsExactlyInAnyOrder(DAY_2, DAY_3);
        ShiftAssignment night = rows.stream().filter(row -> row.getWorkDate().equals(DAY_2)).findFirst().orElseThrow();
        assertThat(night.getShiftCode().getRole().getName()).isEqualTo("TITULAR");
        assertThat(night.getShiftCode().totalHours()).isEqualTo(12);
    }

    @Test
    void deleteByWorkerInRangeDeletesOnlyThatWorkerInsideTheRange() {
        saveDays(ANA_ID, DAY_1, DAY_2, DAY_3, DAY_4);
        saveDays(BRUNO_ID, DAY_2, DAY_3);
        clearContext();

        int deleted = assignmentRepository.deleteByWorkerInRange(ANA_ID, DAY_2, DAY_3);
        clearContext();

        assertThat(deleted).isEqualTo(2);
        assertThat(assignmentRepository.deleteByWorkerInRange(ANA_ID, DAY_2, DAY_3)).as("nothing left to delete").isZero();
        List<ShiftAssignment> left = assignmentRepository.findByWorkDateBetween(DAY_1, DAY_4);
        assertThat(left).filteredOn(row -> row.getWorkerId().equals(ANA_ID))
                .extracting(ShiftAssignment::getWorkDate).containsExactlyInAnyOrder(DAY_1, DAY_4);
        assertThat(left).filteredOn(row -> row.getWorkerId().equals(BRUNO_ID)).hasSize(2);
    }

    @Test
    void rowsCanBeSavedAgainForTheDeletedDaysInTheSameTransaction() {
        saveDays(ANA_ID, DAY_1, DAY_2);
        clearContext();

        assignmentRepository.deleteByWorkerInRange(ANA_ID, DAY_1, DAY_2);
        assignmentRepository.saveAll(List.of(
                new ShiftAssignment(ANA_ID, DAY_1, noche, null),
                new ShiftAssignment(ANA_ID, DAY_2, descanso, null)));

        // The query flushes the new INSERTs: the unique (worker, date) constraint is not broken
        assertThat(assignmentRepository.findByWorkDateBetween(DAY_1, DAY_2))
                .extracting(row -> row.getShiftCode().getCode())
                .containsExactlyInAnyOrder("NOCHE", "DESCANSO");
    }

    private void saveDays(UUID workerId, LocalDate... days) {
        for (LocalDate day : days) {
            assignmentRepository.save(new ShiftAssignment(workerId, day, dia, null));
        }
    }

    private void clearContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
