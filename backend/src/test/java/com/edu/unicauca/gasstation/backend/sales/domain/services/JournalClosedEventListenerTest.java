package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(JournalClosedEventListenerTest.TestConfig.class)
@DisplayName("JournalClosedEvent - Cross-module Event Publication & Listener Reception (S3-04)")
class JournalClosedEventListenerTest {

    @Autowired
    private JournalService journalService;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private TestJournalClosedListener testListener;

    @Test
    @DisplayName("Test listener receives JournalClosedEvent with journalDate and consecutive when journal closes")
    void listenerReceivesJournalClosedEventOnJournalClose() {
        testListener.clear();
        LocalDate journalDate = LocalDate.of(2026, 8, 5);

        JournalStep step6 = JournalStep.builder()
                .id(60L)
                .stepNumber(6)
                .name("Medición final y cuadre")
                .status(StepStatus.PENDING)
                .build();

        Journal openJournal = Journal.builder()
                .id(15L)
                .date(journalDate)
                .consecutive(105L)
                .status(JournalStatus.OPEN)
                .steps(new ArrayList<>(List.of(step6)))
                .build();

        when(journalRepository.findById(15L)).thenReturn(Optional.of(openJournal));
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Journal closed = journalService.closeJournal(15L);

        assertThat(closed.getStatus()).isEqualTo(JournalStatus.CLOSED);
        assertThat(step6.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(step6.getCompletedAt()).isNotNull();

        assertThat(testListener.receivedEvents()).hasSize(1);
        JournalClosedEvent event = testListener.receivedEvents().get(0);
        assertThat(event.journalId()).isEqualTo(15L);
        assertThat(event.journalDate()).isEqualTo(journalDate);
        assertThat(event.date()).isEqualTo(journalDate);
        assertThat(event.consecutive()).isEqualTo(105L);
        assertThat(event.planillaNumber()).isEqualTo(105L);
        assertThat(event.closedAt()).isNotNull();
    }

    @Configuration
    static class TestConfig {

        @Bean
        JournalRepository journalRepository() {
            return Mockito.mock(JournalRepository.class);
        }

        @Bean
        JournalService journalService(JournalRepository journalRepository, ApplicationEventPublisher publisher) {
            return new JournalService(journalRepository, publisher);
        }

        @Bean
        TestJournalClosedListener testJournalClosedListener() {
            return new TestJournalClosedListener();
        }
    }

    static class TestJournalClosedListener {
        private final List<JournalClosedEvent> events = new ArrayList<>();

        @EventListener
        public void onJournalClosed(JournalClosedEvent event) {
            events.add(event);
        }

        List<JournalClosedEvent> receivedEvents() {
            return events;
        }

        void clear() {
            events.clear();
        }
    }
}
