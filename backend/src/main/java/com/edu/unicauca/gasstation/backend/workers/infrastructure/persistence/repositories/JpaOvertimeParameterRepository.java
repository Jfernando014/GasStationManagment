package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.OvertimeParameterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository used by {@code OvertimeParameterRepositoryImpl}. */
public interface JpaOvertimeParameterRepository extends JpaRepository<OvertimeParameterEntity, Long> {

    List<OvertimeParameterEntity> findAllByOrderByValidFromAsc();
}
