package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.inventory.TankTestData;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters.TankRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.TankPersistenceMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({TankRepositoryAdapter.class, TankPersistenceMapper.class})
class TankRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TankRepository tankRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savePersistsAllFieldsAndGeneratesId() {
        Tank saved = tankRepository.save(TankTestData.tank(null, "T1C1", "Tanque 1 Comp. 1"));
        entityManager.flush();
        entityManager.clear();

        Tank found = tankRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getCode()).isEqualTo("T1C1");
        assertThat(found.getFuelType()).isEqualTo(FuelType.MOTOR);
        assertThat(found.getMaxCapacityGallons()).isEqualByComparingTo("3090");
        assertThat(found.getMaxHeightCm()).isEqualByComparingTo("238");
        assertThat(found.getToleranceCm()).isEqualByComparingTo("1");
    }

    @Test
    void existsByCodeIsTrueOnlyForSavedCodes() {
        tankRepository.save(TankTestData.tank(null, "T1C1", "Tanque 1 Comp. 1"));

        assertThat(tankRepository.existsByCode("T1C1")).isTrue();
        assertThat(tankRepository.existsByCode("T9C9")).isFalse();
    }

    @Test
    void findAllSortedByCodeReturnsTanksInCodeOrder() {
        tankRepository.save(TankTestData.tank(null, "T2C1", "Tanque 2 Comp. 1"));
        tankRepository.save(TankTestData.tank(null, "T1C1", "Tanque 1 Comp. 1"));

        List<Tank> tanks = tankRepository.findAllOrderByCode();

        assertThat(tanks).extracting("code").containsExactly("T1C1", "T2C1");
    }

    @Test
    void saveRejectsDuplicateCode() {
        tankRepository.save(TankTestData.tank(null, "T1C1", "Tanque 1 Comp. 1"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            tankRepository.save(TankTestData.tank(null, "T1C1", "Duplicado"));
            entityManager.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}