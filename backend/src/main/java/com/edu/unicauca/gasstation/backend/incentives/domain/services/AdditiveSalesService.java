package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSalesDay;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.AdditiveSaleRepository;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.JournalSyncRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Reads the additive sales stored per journal date and reprocesses a date by hand. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdditiveSalesService {

    private final AdditiveSalesRegistrationService registration;
    private final AdditiveSaleRepository saleRepository;
    private final JournalSyncRepository syncRepository;

    /** State and rows of the date; fails if the date was never registered. */
    public AdditiveSalesDay getDay(LocalDate journalDate) {
        JournalSync sync = syncRepository.findByDate(journalDate)
                .orElseThrow(() -> new IncentiveException(IncentiveError.ADDITIVE_SALES_NOT_REGISTERED, journalDate));
        return new AdditiveSalesDay(sync, saleRepository.findByDate(journalDate));
    }

    /** Dates that failed to process, oldest first. */
    public List<JournalSync> listPending() {
        return syncRepository.findPending();
    }

    /** Processes the date again with the current sales data; on failure the date is left PENDING. */
    public AdditiveSalesDay reprocess(LocalDate journalDate) {
        try {
            registration.reprocess(journalDate);
        } catch (IncentiveException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Could not reprocess the additive sales of {}", journalDate, e);
            markPendingQuietly(journalDate, e);
            throw new IncentiveException(IncentiveError.ADDITIVE_SALES_PROCESSING_FAILED, journalDate);
        }
        return getDay(journalDate);
    }

    private void markPendingQuietly(LocalDate journalDate, RuntimeException cause) {
        try {
            registration.markPending(JournalClosure.ofDate(journalDate), cause);
        } catch (RuntimeException e) {
            log.error("Could not mark the additive sales of {} as pending", journalDate, e);
        }
    }
}
