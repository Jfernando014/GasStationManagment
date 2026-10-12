package com.edu.unicauca.gasstation.backend.sales.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.sales.api.dto.CreateSalesTargetRequest;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SalesTargetApiMapperTest {
    private final SalesTargetApiMapper mapper = new SalesTargetApiMapper();

    @Test
    void mapsAnnualAndMonthlyWithoutChangingDecimalOrMetadata() {
        for (Integer month : new Integer[]{null, 8}) {
            var value = new BigDecimal("1.2300000000000");
            var target = mapper.toDomain(new CreateSalesTargetRequest(2026, month, SalesTargetCategory.BASKET, value, SalesTargetUnit.COP));
            assertThat(target.value()).isSameAs(value);
            assertThat(target.value().scale()).isEqualTo(13);
            assertThat(target.identity().period().month()).isEqualTo(month);
            var created = Instant.parse("2026-08-01T00:00:00Z");
            var response = mapper.toResponse(new StoredSalesTarget(9L, target, created));
            assertThat(response.id()).isEqualTo(9L);
            assertThat(response.createdAt()).isEqualTo(created);
            assertThat(response.value()).isSameAs(value);
            assertThat(response.month()).isEqualTo(month);
        }
    }

    @Test
    void delegatesCategoryUnitInvariantToDomain() {
        assertThatThrownBy(() -> mapper.toDomain(new CreateSalesTargetRequest(2026, null,
                SalesTargetCategory.FUEL_AGGREGATE, BigDecimal.ONE, SalesTargetUnit.COP)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
