package com.edu.unicauca.gasstation.backend.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * The event is kept as JSON in event_publication until every listener finishes, and read back when an incomplete
 * publication is republished. This mapper is configured like the one of the application.
 */
class JournalClosedEventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final JournalClosedEvent event =
            new JournalClosedEvent(7L, LocalDate.of(2026, 8, 1), 288L, LocalDateTime.of(2026, 8, 1, 20, 15, 30));

    @Test
    void survivesAJsonRoundTrip() throws JsonProcessingException {
        String json = mapper.writeValueAsString(event);

        assertThat(mapper.readValue(json, JournalClosedEvent.class)).isEqualTo(event);
    }

    @Test
    void carriesTheOperatingDateOfTheJournal() throws JsonProcessingException {
        assertThat(mapper.writeValueAsString(event)).contains("\"journalDate\":\"2026-08-01\"");
    }
}
