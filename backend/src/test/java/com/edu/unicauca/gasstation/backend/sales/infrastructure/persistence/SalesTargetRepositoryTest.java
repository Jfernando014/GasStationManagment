package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetTestData;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateSalesTargetException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SalesTargetRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers.SalesTargetPersistenceMapper;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=validate", "server.port=0", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({SalesTargetService.class, SalesTargetRepositoryAdapter.class, SalesTargetPersistenceMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SalesTargetRepositoryTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired SalesTargetService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void emptyTargets() { jdbc.execute("TRUNCATE TABLE sales_targets"); }

    @Test
    void registersAndQueriesAnnualAndMonthlyWithDatabaseMetadata() {
        var annual = service.register(SalesTargetTestData.fuel(null, "1.2300000000000"));
        var monthly = service.register(SalesTargetTestData.fuel(8, "26920"));
        assertThat(annual.id()).isPositive();
        assertThat(monthly.id()).isNotEqualTo(annual.id());
        assertThat(annual.createdAt()).isEqualTo(jdbc.queryForObject(
                "SELECT created_at FROM sales_targets WHERE id = ?", java.time.OffsetDateTime.class, annual.id()).toInstant());
        assertThat(annual.target().value().compareTo(new BigDecimal("1.2300000000000"))).isZero();
        assertThat(service.findById(annual.id()).orElseThrow().target().value().compareTo(annual.target().value())).isZero();
        assertThat(service.findByIdentity(annual.target().identity()).orElseThrow().id()).isEqualTo(annual.id());
        assertThat(service.findByIdentity(monthly.target().identity()).orElseThrow().id()).isEqualTo(monthly.id());
        assertThat(service.findByYear(2026)).hasSize(2);
        assertThat(service.findByYear(2027)).isEmpty();
        assertThat(service.findById(-1L)).isEmpty();
    }

    @Test
    void translatesAnnualAndMonthlyDuplicatesWithoutReplacingOriginalValues() {
        for (Integer month : new Integer[]{null, 8}) {
            var target = SalesTargetTestData.fuel(month, "1");
            service.register(target);
            assertThatThrownBy(() -> service.register(SalesTargetTestData.fuel(month, "2")))
                    .isInstanceOf(DuplicateSalesTargetException.class);
            assertThat(service.findByIdentity(target.identity()).orElseThrow().target().value().compareTo(BigDecimal.ONE)).isZero();
        }
    }

    @Test
    void acceptsZeroBasketAndExactNumericBoundaries() {
        var basket = new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.annual(2026), SalesTargetCategory.BASKET),
                BigDecimal.ZERO, SalesTargetUnit.COP);
        assertThat(service.register(basket).target().value().compareTo(BigDecimal.ZERO)).isZero();
        for (var value : new String[]{"999999999999999999.999999999999", "1E-12"}) {
            int month = value.startsWith("999") ? 1 : 2;
            var stored = service.register(SalesTargetTestData.fuel(month, value));
            assertThat(stored.target().value().compareTo(new BigDecimal(value))).isZero();
        }
    }

    @Test
    void invalidStorageValuesCreateNoRows() {
        for (var value : new String[]{"1E18", "1E-13"}) {
            assertThatThrownBy(() -> service.register(SalesTargetTestData.fuel(null, value)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(service.findByYear(2026)).isEmpty();
    }

    @Test
    void concurrentRegistrationsProduceOneRowAndOneSpecificDuplicate() throws Exception {
        for (Integer month : new Integer[]{null, 8}) {
            var barrier = new CyclicBarrier(2);
            Callable<String> register = () -> {
                barrier.await(20, TimeUnit.SECONDS);
                try {
                    service.register(SalesTargetTestData.fuel(month, "1"));
                    return "created";
                } catch (DuplicateSalesTargetException ex) {
                    return "duplicate";
                }
            };
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(register);
                var second = executor.submit(register);
                assertThat(java.util.List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder("created", "duplicate");
            }
        }
        assertThat(service.findByYear(2026)).hasSize(2);
    }
}
