package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerDayHoursEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository used by {@code WorkerDayHoursRepositoryImpl}. */
public interface JpaWorkerDayHoursRepository extends JpaRepository<WorkerDayHoursEntity, Long> {

    List<WorkerDayHoursEntity> findByWorkDateOrderByWorkerNameAscWorkerIdAsc(LocalDate workDate);

    List<WorkerDayHoursEntity> findByWorkerIdAndWorkDateBetweenOrderByWorkDateAsc(UUID workerId, LocalDate from,
                                                                                  LocalDate to);

    // Runs right away so the new records of the date do not collide with the old ones on the unique index
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WorkerDayHoursEntity hours where hours.workDate = :workDate")
    void deleteByWorkDate(@Param("workDate") LocalDate workDate);
}
