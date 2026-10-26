package com.edu.unicauca.gasstation.backend.inventory.infrastructure.config;

import com.edu.unicauca.gasstation.backend.inventory.SalesHistoryPort;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

/**
 * TEMPORARY: lets the application start while the Sales module has not provided its adapters.
 * Without sales data the variation reports NO_SALES_DATA. Delete this class when the Sales
 * adapters are merged.
 */
@Configuration
public class SalesPortsFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(SalesSummaryPort.class)
    SalesSummaryPort emptySalesSummaryPort() {
        return journalDate -> Optional.empty();
    }

    @Bean
    @ConditionalOnMissingBean(SalesHistoryPort.class)
    SalesHistoryPort emptySalesHistoryPort() {
        return (journalDate, weeks) -> Map.of();
    }
}