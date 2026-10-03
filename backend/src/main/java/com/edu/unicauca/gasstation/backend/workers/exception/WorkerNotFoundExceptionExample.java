package com.edu.unicauca.gasstation.backend.workers.exception;

/**
 * Example custom domain exception for workers module.
 */
public class WorkerNotFoundExceptionExample extends RuntimeException {

    public WorkerNotFoundExceptionExample(String message) {
        super(message);
    }

    public WorkerNotFoundExceptionExample(Long id) {
        super("Worker with ID " + id + " not found");
    }
}
