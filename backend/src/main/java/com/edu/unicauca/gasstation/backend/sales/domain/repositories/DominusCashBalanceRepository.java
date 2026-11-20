package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashBalance;
import java.time.LocalDate;
import java.util.Optional;

/** Port for the Dominus cash figures of each day. */
public interface DominusCashBalanceRepository {

    Optional<DominusCashBalance> findByDate(LocalDate date);

    /** Inserts the day or replaces the figures already stored for it. */
    DominusCashBalance save(DominusCashBalance balance);

    /** Removes the figures stored for the day, if any. */
    void deleteByDate(LocalDate date);
}
