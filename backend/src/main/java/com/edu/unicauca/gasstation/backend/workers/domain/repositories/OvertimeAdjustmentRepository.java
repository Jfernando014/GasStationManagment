package com.edu.unicauca.gasstation.backend.workers.domain.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeAdjustment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage of the manual adjustments of the overtime sheets. */
public interface OvertimeAdjustmentRepository {

    OvertimeAdjustment save(OvertimeAdjustment adjustment);

    Optional<OvertimeAdjustment> findById(Long id);

    /** Adjustments of one worker in the fortnight that starts on {@code fortnightStart}, oldest first. */
    List<OvertimeAdjustment> findByWorkerAndFortnight(UUID workerId, LocalDate fortnightStart);

    /** Adjustments of every worker in the fortnight that starts on {@code fortnightStart}, oldest first. */
    List<OvertimeAdjustment> findByFortnight(LocalDate fortnightStart);

    void deleteById(Long id);
}
