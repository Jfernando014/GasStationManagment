package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdministrativeExpenseServiceConfiguration {

    @Bean
    AdministrativeExpenseService administrativeExpenseService(AdministrativeExpenseRepository repository) {
        return new AdministrativeExpenseService(repository);
    }
}