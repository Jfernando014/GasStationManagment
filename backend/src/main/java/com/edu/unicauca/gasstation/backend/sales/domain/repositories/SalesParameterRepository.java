package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesParameterRepository {

    /** Value in force on {@code date}: the one with the latest validFrom that is not after the date. */
    Optional<SalesParameterValue> findEffective(SalesParameterKey key, LocalDate date);

    /** Every value of the parameter, newest first. */
    List<SalesParameterValue> history(SalesParameterKey key);

    /** Inserts the value, or replaces the one with the same key and validFrom (to correct a typo). */
    SalesParameterValue register(SalesParameterValue value);
}
