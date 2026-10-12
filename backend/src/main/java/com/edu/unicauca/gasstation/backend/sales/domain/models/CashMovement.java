package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cash movement. Pure domain model: no persistence or framework annotations. */
public class CashMovement {

    private final Long id;
    private final CashMovementType movementType;
    private final LocalDate movementDate;
    private final BigDecimal amount;
    private final String description;

    public CashMovement(Long id, CashMovementType movementType, LocalDate movementDate,
                        BigDecimal amount, String description) {
        this.id = id;
        this.movementType = movementType;
        this.movementDate = movementDate;
        this.amount = amount;
        this.description = description;
    }

    /** A movement that has not been persisted yet (no id). */
    public static CashMovement create(CashMovementType movementType, LocalDate movementDate,
                                      BigDecimal amount, String description) {
        return new CashMovement(null, movementType, movementDate, amount, description);
    }

    /** Amount with the sign that this type of movement applies to the cash balance. */
    public BigDecimal signedAmount() {
        return movementType.signed(amount);
    }

    public Long getId() { return id; }
    public CashMovementType getMovementType() { return movementType; }
    public LocalDate getMovementDate() { return movementDate; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
