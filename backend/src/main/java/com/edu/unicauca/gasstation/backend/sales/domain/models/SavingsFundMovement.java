package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Savings fund movement. Pure domain model: no persistence or framework annotations. */
public class SavingsFundMovement {

    private final Long id;
    private final SavingsFundMovementType movementType;
    private final LocalDate movementDate;
    private final BigDecimal amount;
    private final String description;

    public SavingsFundMovement(Long id, SavingsFundMovementType movementType, LocalDate movementDate,
                               BigDecimal amount, String description) {
        this.id = id;
        this.movementType = movementType;
        this.movementDate = movementDate;
        this.amount = amount;
        this.description = description;
    }

    /** A movement that has not been persisted yet (no id). */
    public static SavingsFundMovement create(SavingsFundMovementType movementType, LocalDate movementDate,
                                             BigDecimal amount, String description) {
        return new SavingsFundMovement(null, movementType, movementDate, amount, description);
    }

    /** Amount with the sign that this type of movement applies to the fund balance. */
    public BigDecimal signedAmount() {
        return movementType.signed(amount);
    }

    public Long getId() { return id; }
    public SavingsFundMovementType getMovementType() { return movementType; }
    public LocalDate getMovementDate() { return movementDate; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
