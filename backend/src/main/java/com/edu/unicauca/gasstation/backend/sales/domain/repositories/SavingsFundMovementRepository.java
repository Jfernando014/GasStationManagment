package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import java.time.LocalDate;
import java.util.List;

public interface SavingsFundMovementRepository {

    SavingsFundMovement save(SavingsFundMovement movement);

    /** Movements up to and including the date. */
    List<SavingsFundMovement> findUpTo(LocalDate date);

    List<SavingsFundMovement> findAll();

    List<SavingsFundMovement> findAllNewestFirst();

    boolean existsContributionOn(LocalDate date);
}
