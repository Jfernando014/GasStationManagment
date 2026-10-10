package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.inventory.FuelPriceTestData;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelPriceRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters.FuelPriceRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.FuelPricePersistenceMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({FuelPriceRepositoryAdapter.class, FuelPricePersistenceMapper.class})
class FuelPriceRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private FuelPriceRepository fuelPriceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findEffectiveReturnsThePriceInForceOnEachDate() {
        fuelPriceRepository.save(FuelPriceTestData.price(null, FuelType.EXTRA, "22490", LocalDate.of(2026, 7, 1)));
        fuelPriceRepository.save(FuelPriceTestData.price(null, FuelType.EXTRA, "20390", LocalDate.of(2026, 8, 4)));

        assertThat(effectivePrice(FuelType.EXTRA, LocalDate.of(2026, 8, 3))).isEqualByComparingTo("22490");
        assertThat(effectivePrice(FuelType.EXTRA, LocalDate.of(2026, 8, 4))).isEqualByComparingTo("20390");
        assertThat(effectivePrice(FuelType.EXTRA, LocalDate.of(2026, 8, 5))).isEqualByComparingTo("20390");
    }

    @Test
    void findEffectiveIsEmptyBeforeTheFirstPrice() {
        fuelPriceRepository.save(FuelPriceTestData.price(null, FuelType.MOTOR, "16330", LocalDate.of(2026, 8, 1)));

        assertThat(fuelPriceRepository.findEffective(FuelType.MOTOR, LocalDate.of(2026, 7, 31)))
                .isEmpty();
    }

    @Test
    void findEffectiveKeepsEachFuelTypeIndependent() {
        fuelPriceRepository.save(FuelPriceTestData.price(null, FuelType.MOTOR, "16330", LocalDate.of(2026, 8, 1)));
        fuelPriceRepository.save(FuelPriceTestData.price(null, FuelType.EXTRA, "20390", LocalDate.of(2026, 8, 4)));

        assertThat(effectivePrice(FuelType.MOTOR, LocalDate.of(2026, 8, 5))).isEqualByComparingTo("16330");
    }

    @Test
    void saveRejectsTwoPricesForTheSameFuelAndDate() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        fuelPriceRepository.save(FuelPriceTestData.price(null, FuelType.MOTOR, "16330", date));
        entityManager.flush();

        assertThatThrownBy(() -> {
            fuelPriceRepository.save(
                    FuelPriceTestData.price(null, FuelType.MOTOR, "17000", date));
            entityManager.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private BigDecimal effectivePrice(FuelType fuelType, LocalDate date) {
        return fuelPriceRepository
                .findEffective(fuelType, date)
                .orElseThrow()
                .getPricePerGallon();
    }
}