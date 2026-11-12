package com.edu.unicauca.gasstation.backend.workers.domain.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Storage of the hours worked by each worker on each closed day. */
public interface WorkerDayHoursRepository {

    /** Deletes the records of the date and stores these, in the transaction of the caller. */
    void replaceDay(LocalDate workDate, List<WorkerDayHours> records);

    /** Records of the date ordered by worker name. */
    List<WorkerDayHours> findByDate(LocalDate workDate);

    /** Records of one worker from {@code from} to {@code to}, both included, ordered by date. */
    List<WorkerDayHours> findByWorkerBetween(UUID workerId, LocalDate from, LocalDate to);
}
