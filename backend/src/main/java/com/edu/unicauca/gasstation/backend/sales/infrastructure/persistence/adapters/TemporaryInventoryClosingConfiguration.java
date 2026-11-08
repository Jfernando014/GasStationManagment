package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TEMPORARY fallback configuration for {@link InventoryClosingReadPort}.
 * Automatically backs off once the {@code inventory} module publishes its {@code inventoryQuery} adapter (S3-19).
 */
@Configuration
public class TemporaryInventoryClosingConfiguration {

    @Bean
    @ConditionalOnMissingBean(InventoryClosingReadPort.class)
    public InventoryClosingReadPort temporaryInventoryClosingReadPort() {
        return journalDate -> InventoryClosingReadPort.ClosingInventorySnapshot.empty();
    }
}
