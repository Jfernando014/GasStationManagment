package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.JournalSyncRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClosedJournalAdapterTest {

    @Mock
    private JournalSyncRepository syncRepository;

    @InjectMocks
    private ClosedJournalAdapter adapter;

    @Test
    void theLastClosedJournalIsTheLatestDateWithAProcessingState() {
        when(syncRepository.findLastDate()).thenReturn(Optional.of(LocalDate.of(2026, 4, 18)));

        assertThat(adapter.lastClosedDate()).contains(LocalDate.of(2026, 4, 18));
        verify(syncRepository).findLastDate();
    }

    @Test
    void withoutAnyProcessedClosingThereIsNoClosedJournal() {
        when(syncRepository.findLastDate()).thenReturn(Optional.empty());

        assertThat(adapter.lastClosedDate()).isEmpty();
    }
}
