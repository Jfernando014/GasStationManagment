package com.edu.unicauca.gasstation.backend.sales.exception;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetIdentity;

public class DuplicateSalesTargetException extends RuntimeException {
    public DuplicateSalesTargetException(SalesTargetIdentity identity, Throwable cause) {
        super("A configured sales target already exists for " + identity, cause);
    }
}
