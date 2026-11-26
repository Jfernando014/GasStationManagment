package com.edu.unicauca.gasstation.backend.sales.infrastructure.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelMonthlyTargetReader;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * TEMPORARY implementation of {@link FuelMonthlyTargetReader} while Metas does not provide the fuel targets.
 * Delete this class and {@link DevStubFuelMonthlyTargetReader} when the real implementation is added.
 *
 * <p>By default it reads nothing. The invented targets of the stub require both the dev profile and
 * {@code sales.fuel-targets.dev-stub.enabled=true}.
 */
@Slf4j
@Configuration
public class TemporaryFuelMonthlyTargetConfiguration {

    static final String STUB_PROPERTY = "sales.fuel-targets.dev-stub.enabled";

    @Bean
    public FuelMonthlyTargetReader temporaryFuelMonthlyTargetReader(Environment environment) {
        boolean stub = environment.getProperty(STUB_PROPERTY, Boolean.class, false)
                && environment.acceptsProfiles(Profiles.of("dev"));
        if (stub) {
            log.warn("TEMPORARY: fuel targets are invented example values ({}=true)", STUB_PROPERTY);
            return new DevStubFuelMonthlyTargetReader();
        }
        return month -> Map.of();
    }
}
