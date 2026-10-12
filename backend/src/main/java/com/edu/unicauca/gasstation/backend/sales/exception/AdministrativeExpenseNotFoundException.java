package com.edu.unicauca.gasstation.backend.sales.exception;

public class AdministrativeExpenseNotFoundException extends RuntimeException {

    public AdministrativeExpenseNotFoundException(Long id) {
        super("No existe el gasto administrativo con id " + id);
    }
}