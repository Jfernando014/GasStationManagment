package com.edu.unicauca.gasstation.backend.incentives.infrastructure.config;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AccumulationStart;
import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Settings of the monthly calculation. {@code incentives.accumulated-from} is the first month, as yyyy-MM. */
@Configuration
class IncentiveCalculationConfiguration {

    @Bean
    AccumulationStart accumulationStart(@Value("${incentives.accumulated-from}") String accumulatedFrom) {
        return new AccumulationStart(YearMonth.parse(accumulatedFrom.strip()));
    }
}
