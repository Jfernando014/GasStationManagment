package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import com.edu.unicauca.gasstation.backend.workers.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerHoursRange;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.HoursSyncRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerDayHoursRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.DayHoursNotRegisteredException;
import com.edu.unicauca.gasstation.backend.workers.exception.DayHoursProcessingException;
import com.edu.unicauca.gasstation.backend.workers.exception.InvalidHoursRangeException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Reads the hours kept per closed day and reprocesses a date by hand. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerHoursService {

    /** Longest range of dates accepted for the hours of a worker, both ends included. */
    public static final int MAX_RANGE_DAYS = 62;

    private final WorkerHoursRegistrationService registration;
    private final WorkerDayHoursRepository hoursRepository;
    private final HoursSyncRepository syncRepository;
    private final WorkerRepository workerRepository;

    /** Hours of every worker of the date and the state of the date; fails if the date was never registered. */
    public DayHours getDay(LocalDate workDate) {
        HoursSync sync = syncRepository.findByDate(workDate)
                .orElseThrow(() -> new DayHoursNotRegisteredException(workDate));
        return new DayHours(sync, hoursRepository.findByDate(workDate));
    }

    /** Dates that failed to process, oldest first. */
    public List<HoursSync> listPending() {
        return syncRepository.findPending();
    }

    /** Stored days of one worker in the range, in date order, with their total and the pending dates of the range. */
    public WorkerHoursRange getWorkerHours(UUID workerId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new InvalidHoursRangeException("La fecha final no puede ser anterior a la fecha inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new InvalidHoursRangeException("El rango no puede superar " + MAX_RANGE_DAYS + " días");
        }
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new WorkerNotFoundException(workerId));
        List<WorkerDayHours> days = hoursRepository.findByWorkerBetween(workerId, from, to);
        int total = days.stream().mapToInt(WorkerDayHours::totalHours).sum();
        return new WorkerHoursRange(workerId, worker.getFullName(), from, to, total, days,
                syncRepository.findPendingDatesBetween(from, to));
    }

    /** Processes the date again with the day staff as it is now; on failure the date is left PENDING. */
    public DayHours reprocess(LocalDate workDate) {
        try {
            registration.reprocess(workDate);
        } catch (DayHoursNotRegisteredException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Could not reprocess the worked hours of {}", workDate, e);
            markPendingQuietly(workDate, e);
            throw new DayHoursProcessingException(workDate);
        }
        return getDay(workDate);
    }

    private void markPendingQuietly(LocalDate workDate, RuntimeException cause) {
        try {
            registration.markPending(JournalClosure.ofDate(workDate), cause);
        } catch (RuntimeException e) {
            log.error("Could not mark the worked hours of {} as pending", workDate, e);
        }
    }
}
