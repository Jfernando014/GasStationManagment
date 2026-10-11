package com.edu.unicauca.gasstation.backend.sales.exception;

public class AdministrativeExpenseNotFoundException extends RuntimeException {

    public AdministrativeExpenseNotFoundException(Long id) {
        super("Administrative expense not found with id: " + id);
    }
}
