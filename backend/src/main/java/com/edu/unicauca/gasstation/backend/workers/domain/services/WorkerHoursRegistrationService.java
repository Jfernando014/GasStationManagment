package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursReading;
import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import com.edu.unicauca.gasstation.backend.workers.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.ports.DayStaffPort;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.HoursSyncRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerDayHoursRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.DayHoursNotRegisteredException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the hours worked by each worker on a closed day. Each operation runs in its own transaction: the journal
 * closing that triggers it is already committed and must not be affected by what happens here.
 */
@Service
@RequiredArgsConstructor
public class WorkerHoursRegistrationService {

    private final DayStaffPort dayStaffPort;
    private final WorkerDayHoursRepository hoursRepository;
    private final HoursSyncRepository syncRepository;
    private final WorkerRepository workerRepository;
    private final Clock clock;

    /**
     * Replaces what is kept of the closed journal's date with the day staff as it is now and marks the date
     * PROCESSED. The records of workers who are inactive and no longer come from the day staff are kept as they
     * were. A day whose novelties were not applied is kept with the grid and marked as such. Any failure undoes
     * everything.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HoursSync registerClosedJournal(JournalClosure closure) {
        return store(closure);
    }

    /**
     * Processes again, with the day staff as it is now, a date whose closing already arrived (PROCESSED or
     * PENDING), exactly as the closing does, so the records of inactive workers are kept too. A date without state is
     * refused: it was never closed. Any failure undoes everything.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HoursSync reprocess(LocalDate workDate) {
        if (syncRepository.findByDate(workDate).isEmpty()) {
            throw new DayHoursNotRegisteredException(workDate);
        }
        return store(JournalClosure.ofDate(workDate));
    }

    /** Marks the date PENDING with the reason, in a transaction apart from the one that failed. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HoursSync markPending(JournalClosure closure, RuntimeException cause) {
        HoursSync previous = syncRepository.findByDate(closure.workDate()).orElse(null);
        return syncRepository.save(HoursSync.pending(previous, closure, describe(cause), Instant.now(clock)));
    }

    private HoursSync store(JournalClosure closure) {
        HoursSync previous = syncRepository.findByDate(closure.workDate()).orElse(null);
        DayHoursReading reading = dayStaffPort.read(closure.workDate());
        Long journalId = closure.journalId() != null || previous == null ? closure.journalId() : previous.journalId();
        List<WorkerDayHours> records = new ArrayList<>(reading.records().stream()
                .map(record -> record.withJournalId(journalId)).toList());
        records.addAll(recordsOfInactiveWorkers(closure.workDate(), reading.records()));
        hoursRepository.replaceDay(closure.workDate(), records);
        return syncRepository.save(HoursSync.processed(previous, closure, reading.noveltiesApplied(),
                Instant.now(clock)));
    }

    /**
     * Records already kept for the date whose worker is missing from the new reading because the worker is inactive:
     * the shifts module leaves inactive workers out, so replacing the day would lose hours that were really worked.
     * An active worker missing from the reading (for example, because a novelty was removed) is not kept.
     */
    private List<WorkerDayHours> recordsOfInactiveWorkers(LocalDate workDate, List<WorkerDayHours> reading) {
        Set<UUID> read = reading.stream().map(WorkerDayHours::workerId).collect(Collectors.toSet());
        List<WorkerDayHours> missing = hoursRepository.findByDate(workDate).stream()
                .filter(record -> !read.contains(record.workerId()))
                .toList();
        if (missing.isEmpty()) {
            return List.of();
        }
        Set<UUID> inactive = workerRepository
                .findAllByIds(missing.stream().map(WorkerDayHours::workerId).collect(Collectors.toSet())).stream()
                .filter(worker -> !worker.isActive())
                .map(Worker::getId)
                .collect(Collectors.toSet());
        return missing.stream().filter(record -> inactive.contains(record.workerId())).toList();
    }

    private static String describe(RuntimeException cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message.strip();
    }
}
