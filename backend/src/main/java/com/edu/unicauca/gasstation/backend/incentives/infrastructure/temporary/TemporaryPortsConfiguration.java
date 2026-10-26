package com.edu.unicauca.gasstation.backend.incentives.infrastructure.temporary;

import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveCatalogPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.ClosedJournalPort;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TEMPORARY. Registers the in-memory ports only if no other bean of their type exists.
 * {@code incentives.temporary-closed-journal.last-closed-date} sets the last closed date for manual tests.
 */
@Configuration
class TemporaryPortsConfiguration {

    @Bean
    @ConditionalOnMissingBean(AdditiveCatalogPort.class)
    AdditiveCatalogPort temporaryAdditiveCatalog() {
        return new TemporaryAdditiveCatalog();
    }

    @Bean
    @ConditionalOnMissingBean(ClosedJournalPort.class)
    ClosedJournalPort temporaryClosedJournal(
            @Value("${incentives.temporary-closed-journal.last-closed-date:}") String lastClosedDate) {
        return new TemporaryClosedJournal(lastClosedDate.isBlank() ? null : LocalDate.parse(lastClosedDate.strip()));
    }
}
