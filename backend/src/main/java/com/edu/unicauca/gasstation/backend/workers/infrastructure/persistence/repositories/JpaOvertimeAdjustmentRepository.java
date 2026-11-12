package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.OvertimeAdjustmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository used by {@code OvertimeAdjustmentRepositoryImpl}. */
public interface JpaOvertimeAdjustmentRepository extends JpaRepository<OvertimeAdjustmentEntity, Long> {

    List<OvertimeAdjustmentEntity> findByWorkerIdAndFortnightStartOrderByCreatedAtAscIdAsc(UUID workerId,
                                                                                         LocalDate fortnightStart);

    List<OvertimeAdjustmentEntity> findByFortnightStartOrderByCreatedAtAscIdAsc(LocalDate fortnightStart);
}
