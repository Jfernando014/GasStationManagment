package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The replacement, or the other worker of a swap, is the same worker. Translated to 400.
 */
public class SameWorkerNoveltyException extends RuntimeException {

    public SameWorkerNoveltyException() {
        super("El reemplazante o el vendedor del intercambio no puede ser el mismo vendedor");
    }
}
