package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesParameterNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsContributionOnJournalClosedTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 1);
    private static final JournalClosedEvent EVENT =
            new JournalClosedEvent(7L, DAY, 288L, LocalDateTime.of(2026, 8, 1, 20, 0));

    @Mock
    private SalesParameterService parameters;
    @Mock
    private SavingsFundService savingsFund;

    private SavingsContributionOnJournalClosed listener;

    @BeforeEach
    void setUp() {
        listener = new SavingsContributionOnJournalClosed(parameters, savingsFund);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    private void givenDailySavings(LocalDate date, long amount) {
        when(parameters.effective(SalesParameterKey.DAILY_SAVINGS, date))
                .thenReturn(new SalesParameterValue(SalesParameterKey.DAILY_SAVINGS, cop(amount),
                        LocalDate.of(2026, 1, 1)));
    }

    private static SavingsFundMovement contribution(long amount) {
        return SavingsFundMovement.create(SavingsFundMovementType.CONTRIBUTION, DAY, cop(amount), null);
    }

    @Test
    void registersTheDailySavingsOfTheDayThatWasClosed() {
        givenDailySavings(DAY, 250_000);
        when(savingsFund.registerContributionIfAbsent(DAY, cop(250_000),
                SavingsContributionOnJournalClosed.DESCRIPTION)).thenReturn(Optional.of(contribution(250_000)));

        listener.on(EVENT);

        verify(savingsFund).registerContributionIfAbsent(DAY, cop(250_000),
                SavingsContributionOnJournalClosed.DESCRIPTION);
    }

    @Test
    void usesTheAmountInForceOnTheDateOfTheJournal() {
        LocalDate later = LocalDate.of(2026, 9, 2);
        givenDailySavings(later, 300_000);
        when(savingsFund.registerContributionIfAbsent(later, cop(300_000),
                SavingsContributionOnJournalClosed.DESCRIPTION)).thenReturn(Optional.empty());

        listener.on(new JournalClosedEvent(8L, later, 289L, LocalDateTime.of(2026, 9, 2, 20, 0)));

        verify(parameters).effective(SalesParameterKey.DAILY_SAVINGS, later);
        verify(savingsFund).registerContributionIfAbsent(later, cop(300_000),
                SavingsContributionOnJournalClosed.DESCRIPTION);
    }

    @Test
    void aDayThatAlreadyHasItsContributionIsNotAnError() {
        givenDailySavings(DAY, 250_000);
        when(savingsFund.registerContributionIfAbsent(any(), any(), any())).thenReturn(Optional.empty());

        assertThatCode(() -> listener.on(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void withoutAParameterInForceItFailsAndRegistersNothing() {
        when(parameters.effective(SalesParameterKey.DAILY_SAVINGS, DAY))
                .thenThrow(new SalesParameterNotFoundException("No hay un valor de 'daily-savings' vigente"));

        assertThatThrownBy(() -> listener.on(EVENT)).isInstanceOf(SalesParameterNotFoundException.class);
        verify(savingsFund, never()).registerContributionIfAbsent(any(), any(), any());
        assertThat(SavingsContributionOnJournalClosed.DESCRIPTION).isNotBlank();
    }
}
