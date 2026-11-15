package com.edu.unicauca.gasstation.backend.workers.domain.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameters;
import java.util.List;

/** Storage of the versions of the overtime parameters. Versions are only added. */
public interface OvertimeParameterRepository {

    /** Every version, whatever its validity. */
    List<OvertimeParameters> findAll();

    /**
     * Stores a new version.
     *
     * @throws com.edu.unicauca.gasstation.backend.workers.exception.ParameterVersionExistsException if another
     *         version starts the same day
     */
    OvertimeParameters save(OvertimeParameters parameters);
}
