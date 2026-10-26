package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.AutonomyThresholdPersistenceMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "server.port=0")
@Import({AutonomyThresholdRepositoryAdapter.class, AutonomyThresholdPersistenceMapper.class})
@Testcontainers
class AutonomyThresholdRepositoryAdapterTest {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AutonomyThresholdRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        adapter.save(threshold(AUG_1, "5.0", "2.0"));
        adapter.save(threshold(SEP_1, "7.0", "3.0"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void savePersistsAllTheFields() {
        AutonomyThreshold saved = adapter.save(threshold(LocalDate.of(2026, 10, 1), "6.5", "2.5"));
        entityManager.flush();
        entityManager.clear();

        AutonomyThreshold found = adapter.findEffectiveOn(LocalDate.of(2026, 10, 1)).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getValidFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(found.getWarningDays()).isEqualByComparingTo("6.5");
        assertThat(found.getCriticalDays()).isEqualByComparingTo("2.5");
        assertThat(found.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 0));
    }

    @Test
    void findEffectiveOnReturnsTheLatestThresholdNotAfterTheDate() {
        AutonomyThreshold found = adapter.findEffectiveOn(LocalDate.of(2026, 8, 31)).orElseThrow();

        assertThat(found.getValidFrom()).isEqualTo(AUG_1);
        assertThat(found.getWarningDays()).isEqualByComparingTo("5.0");
    }

    @Test
    void findEffectiveOnIsInclusiveOnTheValidityDate() {
        assertThat(adapter.findEffectiveOn(SEP_1).orElseThrow().getWarningDays()).isEqualByComparingTo("7.0");
    }

    @Test
    void findEffectiveOnReturnsEmptyBeforeTheFirstValidity() {
        assertThat(adapter.findEffectiveOn(LocalDate.of(2026, 7, 31))).isEmpty();
    }

    @Test
    void existsByValidFromTellsIfTheDateIsTaken() {
        assertThat(adapter.existsByValidFrom(AUG_1)).isTrue();
        assertThat(adapter.existsByValidFrom(LocalDate.of(2026, 8, 2))).isFalse();
    }

    @Test
    void findAllReturnsTheMostRecentValidityFirst() {
        assertThat(adapter.findAll()).extracting(AutonomyThreshold::getValidFrom).containsExactly(SEP_1, AUG_1);
    }

    @Test
    void theDatabaseRejectsAnotherThresholdWithTheSameValidityDate() {
        assertThatThrownBy(() -> adapter.save(threshold(AUG_1, "9.0", "4.0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsACriticalThresholdThatIsNotLowerThanTheWarning() {
        assertThatThrownBy(() -> adapter.save(threshold(LocalDate.of(2026, 11, 1), "2.0", "5.0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AutonomyThreshold threshold(LocalDate validFrom, String warning, String critical) {
        AutonomyThreshold threshold = new AutonomyThreshold();
        threshold.setValidFrom(validFrom);
        threshold.setWarningDays(new BigDecimal(warning));
        threshold.setCriticalDays(new BigDecimal(critical));
        threshold.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));
        return threshold;
    }
}