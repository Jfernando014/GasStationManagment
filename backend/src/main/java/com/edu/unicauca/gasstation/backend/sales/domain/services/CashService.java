package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CashService {

    CashMovement register(CashMovementType type, LocalDate date, BigDecimal amount, String description);

    BigDecimal getBalanceAt(LocalDate date);

    List<CashMovement> listMovements();
}
