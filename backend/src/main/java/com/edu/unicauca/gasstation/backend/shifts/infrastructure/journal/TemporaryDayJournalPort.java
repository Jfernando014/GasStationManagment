package com.edu.unicauca.gasstation.backend.shifts.infrastructure.journal;

import com.edu.unicauca.gasstation.backend.shifts.DayJournalPort;
import com.edu.unicauca.gasstation.backend.shifts.Step1Status;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * TEMPORARY. In-memory {@link DayJournalPort} used only while the {@code sales} module does not publish its
 * implementation by date. Delete it, with {@link TemporaryDayJournalConfiguration}, when the real adapter
 * exists.
 *
 * <p>Every date has a journal and it is editable, except the dates configured as closed. Step 1 starts PENDING; a
 * confirmation is only remembered in the memory of the process (it is lost on restart) and is undone if the
 * transaction that made it rolls back.
 */
class TemporaryDayJournalPort implements DayJournalPort {

    private final Set<LocalDate> closedDates;
    private final Set<LocalDate> confirmedDates = ConcurrentHashMap.newKeySet();

    TemporaryDayJournalPort(Set<LocalDate> closedDates) {
        this.closedDates = Set.copyOf(closedDates);
    }

    @Override
    public Step1Status step1Status(LocalDate date) {
        return confirmedDates.contains(date) ? Step1Status.CONFIRMED : Step1Status.PENDING;
    }

    @Override
    public boolean isEditable(LocalDate date) {
        return !closedDates.contains(date);
    }

    @Override
    public void markStep1Completed(LocalDate date) {
        if (!isEditable(date)) {
            throw new IllegalStateException("The journal of " + date + " is closed");
        }
        boolean added = confirmedDates.add(date);
        if (added && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        confirmedDates.remove(date);
                    }
                }
            });
        }
    }
}
