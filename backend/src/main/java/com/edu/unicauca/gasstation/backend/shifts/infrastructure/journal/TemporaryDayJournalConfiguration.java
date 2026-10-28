package com.edu.unicauca.gasstation.backend.shifts.infrastructure.journal;

import com.edu.unicauca.gasstation.backend.shifts.spi.DayJournalPort;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TEMPORARY. Registers {@link TemporaryDayJournalPort} only when no other {@link DayJournalPort} exists, that is,
 * until the {@code sales} module publishes its adapter. Delete this class with the port when that happens.
 *
 * <p>{@code shifts.temporary-day-journal.closed-dates} (comma separated, yyyy-MM-dd, empty by default) lists the
 * dates whose journal behaves as closed, so the read-only step 1 can be tried by hand.
 */
@Configuration
class TemporaryDayJournalConfiguration {

    @Bean
    @ConditionalOnMissingBean(DayJournalPort.class)
    DayJournalPort temporaryDayJournalPort(
            @Value("${shifts.temporary-day-journal.closed-dates:}") String closedDates) {
        Set<LocalDate> closed = Arrays.stream(closedDates.split(","))
                .map(String::strip)
                .filter(date -> !date.isEmpty())
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
        return new TemporaryDayJournalPort(closed);
    }
}
