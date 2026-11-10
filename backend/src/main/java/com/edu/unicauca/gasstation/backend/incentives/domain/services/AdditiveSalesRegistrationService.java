package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveSalesPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.AdditiveSaleRepository;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.JournalSyncRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores the additive units sold per seller for a journal date. Each operation runs in its own transaction: the
 * journal closing that triggers it is already committed and must not be affected by what happens here.
 */
@Service
@RequiredArgsConstructor
public class AdditiveSalesRegistrationService {

    private final AdditiveSalesPort salesPort;
    private final AdditiveSaleRepository saleRepository;
    private final JournalSyncRepository syncRepository;
    private final Clock clock;

    /**
     * Replaces the rows of the closed journal's date with the current sales data and marks the date PROCESSED. A
     * date for which sales holds no seller close is processed with no rows. Any failure undoes everything.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalSync registerClosedJournal(JournalClosure closure) {
        return store(closure, salesPort.findByDate(closure.journalDate()));
    }

    /**
     * Processes again, with the current sales data, a date whose closing already arrived (PROCESSED or PENDING),
     * exactly as the closing does. A date without state is refused: it was never closed, so its sales are not
     * final. Any failure undoes everything.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalSync reprocess(LocalDate journalDate) {
        if (syncRepository.findByDate(journalDate).isEmpty()) {
            throw new IncentiveException(IncentiveError.ADDITIVE_SALES_NOT_REGISTERED, journalDate);
        }
        return store(JournalClosure.ofDate(journalDate), salesPort.findByDate(journalDate));
    }

    /** Marks the date PENDING with the reason, in a transaction apart from the one that failed. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalSync markPending(JournalClosure closure, RuntimeException cause) {
        JournalSync previous = syncRepository.findByDate(closure.journalDate()).orElse(null);
        return syncRepository.save(JournalSync.pending(previous, closure, describe(cause), Instant.now(clock)));
    }

    private JournalSync store(JournalClosure closure, List<AdditiveSaleUnits> sales) {
        saleRepository.replaceDay(closure.journalDate(), sales);
        JournalSync previous = syncRepository.findByDate(closure.journalDate()).orElse(null);
        return syncRepository.save(JournalSync.processed(previous, closure, Instant.now(clock)));
    }

    private static String describe(RuntimeException cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message.strip();
    }
}
