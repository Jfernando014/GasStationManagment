package com.edu.unicauca.gasstation.backend.backup.infrastructure.config;

import com.edu.unicauca.gasstation.backend.backup.application.MaintenanceMode;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.web.MaintenanceModeFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Enables the backup settings.
 *
 * @author javiersolanop777
 */
@Configuration
@EnableConfigurationProperties(BackupProperties.class)
public class BackupConfiguration {

    @Bean
    public FilterRegistrationBean<MaintenanceModeFilter> maintenanceModeFilter(
            MaintenanceMode prmMaintenanceMode) {
        FilterRegistrationBean<MaintenanceModeFilter> objRegistration = new FilterRegistrationBean<>();
        objRegistration.setFilter(new MaintenanceModeFilter(prmMaintenanceMode));
        objRegistration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        objRegistration.addUrlPatterns("/*");
        return objRegistration;
    }
}
