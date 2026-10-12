package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalFileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalFileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JournalFileServiceConfiguration {

    @Bean
    JournalFileService journalFileService(
            JournalFileRepository journalFileRepository,
            JournalReadPort journalReadPort) {
        return new JournalFileService(journalFileRepository, journalReadPort);
    }
}