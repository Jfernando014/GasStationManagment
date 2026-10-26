package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters.RoleRepositoryImpl;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters.ShiftCodeRepositoryImpl;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers.ShiftCodePersistenceMapper;
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
@Import({ShiftCodeRepositoryImpl.class, ShiftCodePersistenceMapper.class,
        RoleRepositoryImpl.class})
class ShiftCodeRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role titular;
    private Role apoyo;

    @BeforeEach
    void saveRoles() {
        titular = roleRepository.save(new Role(null, "TITULAR", 1));
        apoyo = roleRepository.save(new Role(null, "APOYO", 3));
    }

    @Test
    void savePersistsEveryFieldWithItsRoleAndPairing() {
        ShiftCode dia6 = shiftCodeRepository.save(ShiftTestData.newShiftCode("DIA6", titular, null));
        ShiftCode saved = shiftCodeRepository.save(ShiftTestData.newShiftCode("6-9y5-9", apoyo, dia6.getId()));
        clearContext();

        ShiftCode found = shiftCodeRepository.findByCode("6-9y5-9").orElseThrow();

        assertThat(found).usingRecursiveComparison().isEqualTo(saved);
        assertThat(found.getRole().getName()).isEqualTo("APOYO");
        assertThat(found.getPairedWithId()).isEqualTo(dia6.getId());
        assertThat(found.segments()).hasSize(2);
        assertThat(shiftCodeRepository.findByCode("DIA6").orElseThrow().isRequiresSupport()).isTrue();
        assertThat(shiftCodeRepository.findByCode("XYZ")).isEmpty();
    }

    @Test
    void findActiveExcludesInactiveCodesAndIncludesDescanso() {
        shiftCodeRepository.save(ShiftTestData.newShiftCode("DIA", titular, null));
        shiftCodeRepository.save(ShiftTestData.newShiftCode("DESCANSO", null, null));
        shiftCodeRepository.save(ShiftCode.builder()
                .code("OLD").name("Retired").role(titular).period(ShiftPeriod.DAY)
                .startHour1((short) 7).endHour1((short) 15).active(false).build());
        clearContext();

        assertThat(shiftCodeRepository.findActive()).extracting(ShiftCode::getCode)
                .containsExactlyInAnyOrder("DIA", "DESCANSO");
    }

    @Test
    void codeIsUnique() {
        shiftCodeRepository.save(ShiftTestData.newShiftCode("DIA", titular, null));
        clearContext();
        shiftCodeRepository.save(ShiftTestData.newShiftCode("DIA", titular, null));

        // The query flushes the pending INSERT, which the UNIQUE constraint on code rejects
        assertThatThrownBy(() -> shiftCodeRepository.findByCode("DIA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void clearContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
