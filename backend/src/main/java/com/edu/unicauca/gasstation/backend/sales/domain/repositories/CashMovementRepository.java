package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import java.time.LocalDate;
import java.util.List;

public interface CashMovementRepository {

    CashMovement save(CashMovement movement);

    /** Movements up to and including the date. */
    List<CashMovement> findUpTo(LocalDate date);

    List<CashMovement> findAllNewestFirst();
}
