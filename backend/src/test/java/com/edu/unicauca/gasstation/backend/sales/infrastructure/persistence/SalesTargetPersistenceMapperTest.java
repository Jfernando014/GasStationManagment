package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetTestData;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers.SalesTargetPersistenceMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalesTargetPersistenceMapperTest {
    private final SalesTargetPersistenceMapper mapper = new SalesTargetPersistenceMapper();

    @Test
    void mapsAnnualAndMonthlyPeriodsWithoutSupplyingDatabaseMetadata() {
        for (Integer month : new Integer[]{null, 8}) {
            var target = SalesTargetTestData.fuel(month, "1.2300000000000");
            var entity = mapper.toEntity(target);
            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getTargetYear()).isEqualTo(2026);
            assertThat(entity.getTargetMonth()).isEqualTo(month == null ? null : month.shortValue());
            assertThat(entity.getTargetValue()).isSameAs(target.value());
            entity.setId(3L);
            entity.setCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
            var restored = mapper.toDomain(entity);
            assertThat(restored.id()).isEqualTo(3L);
            assertThat(restored.createdAt()).isEqualTo(entity.getCreatedAt());
            assertThat(restored.target().identity()).isEqualTo(target.identity());
            assertThat(restored.target().value().compareTo(target.value())).isZero();
            assertThat(restored.target().unit()).isEqualTo(target.unit());
        }
    }
}
