package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetTestData;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTarget;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesTargetRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SalesTargetRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers.SalesTargetPersistenceMapper;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.JpaSalesTargetRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesTargetMaximumSaleTest {
    private SalesTarget target(String maximum) {
        var base=SalesTargetTestData.fuel(8,"100");
        return new SalesTarget(base.identity(),base.value(),base.unit(),new BigDecimal(maximum));
    }
    @Test void maximumSalePreservesPrecisionAndHasNoUnspecifiedRelationshipWithTarget() {
        var target=target("1.2300000000000");
        assertThat(target.maximumSale()).isEqualTo(new BigDecimal("1.2300000000000"));
        assertThat(target.maximumSale().scale()).isEqualTo(13);
        assertThat(target.value()).isEqualByComparingTo("100");
        assertThat(target("0").maximumSale()).isZero();
        assertThatThrownBy(()->target("-1")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void serviceRejectsUnrepresentableMaximumBeforeWriting() {
        var repository=mock(SalesTargetRepository.class);
        var service=new SalesTargetService(repository);
        for(var maximum:new String[]{"1E18","1E-13"})
            assertThatThrownBy(()->service.register(target(maximum))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
    @Test void directPortAdapterAlsoRejectsUnrepresentableMaximumBeforeWriting() {
        var repository=mock(JpaSalesTargetRepository.class);
        var entityManager=mock(EntityManager.class);
        var adapter=new SalesTargetRepositoryAdapter(repository,new SalesTargetPersistenceMapper(),entityManager);
        for(var maximum:new String[]{"1E18","1E-13"})
            assertThatThrownBy(()->adapter.register(target(maximum))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository,entityManager);
    }
}
