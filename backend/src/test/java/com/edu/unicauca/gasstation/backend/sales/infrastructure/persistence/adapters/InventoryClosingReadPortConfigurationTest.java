package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InventoryClosingReadPortConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(InventoryClosingQuery.class, () -> mock(InventoryClosingQuery.class))
            .withUserConfiguration(
                    TemporaryInventoryClosingConfiguration.class,
                    InventoryClosingReadPortAdapter.class);

    @Test
    void theRealAdapterReplacesTheTemporaryFallback() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(InventoryClosingReadPort.class);
            assertThat(context.getBean(InventoryClosingReadPort.class))
                    .isInstanceOf(InventoryClosingReadPortAdapter.class);
        });
    }
}
