package com.edu.unicauca.gasstation.backend.shifts.exception;

import org.springframework.http.HttpStatus;

/**
 * Every error of the shifts module: its stable code ({@link #name()}), the HTTP status of the answer and the message
 * in Spanish for the end user, with {@code %s} placeholders filled in by {@link ShiftException}.
 */
public enum ShiftError {

    // Catalog and schedule
    SHIFT_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "No se encontró el código de turno '%s'"),
    PAST_DATE(HttpStatus.BAD_REQUEST, "La fecha %s ya pasó; solo se pueden programar turnos desde hoy"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "La fecha final (%2$s) no puede ser anterior a la fecha inicial (%1$s)"),
    INVALID_ROTATION_LENGTH(HttpStatus.BAD_REQUEST, "La rotación debe cubrir al menos un ciclo completo y como máximo "
            + "un trimestre: si inicia el %s, la fecha final debe estar entre %s y %s"),
    WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, "No se encontró el vendedor con id %s"),
    WORKER_INACTIVE(HttpStatus.CONFLICT, "El vendedor con id %s está inactivo y no se le pueden asignar turnos"),
    ROLE_MISMATCH(HttpStatus.CONFLICT, "El turno %s es para el rol %s y no corresponde al rol del vendedor"),
    ROTATION_NOT_ALLOWED(HttpStatus.CONFLICT,
            "La rotación solo se genera para vendedores titulares; al apoyo se le asignan los días uno a uno"),

    // Day staff
    JOURNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "No existe una jornada para la fecha %s"),
    JOURNAL_NOT_EDITABLE(HttpStatus.CONFLICT,
            "La jornada del %s está cerrada; el personal del día ya no se puede cambiar"),
    INVALID_NOVELTY_REASON(HttpStatus.BAD_REQUEST, "El motivo es obligatorio y debe tener entre 3 y 255 caracteres"),
    INVALID_NOVELTY(HttpStatus.BAD_REQUEST, "%s"),
    SAME_WORKER(HttpStatus.BAD_REQUEST,
            "El reemplazante o el vendedor del intercambio no puede ser el mismo vendedor"),
    SHIFT_HOURS_OVERLAP(HttpStatus.CONFLICT, "%s ya trabaja %s ese día y se cruza en horas con %s"),
    WORKER_ALREADY_WORKING(HttpStatus.CONFLICT,
            "%s ya trabaja %s ese día; un vendedor tiene a lo sumo un código por día"),
    EXCEPTIONAL_COVERAGE_REQUIRED(HttpStatus.CONFLICT, "El turno %2$s es de %3$s y no corresponde al rol de %1$s; "
            + "márquelo como cobertura excepcional"),
    NOVELTY_CONFLICT(HttpStatus.CONFLICT, "%s ya tiene una novedad ese día; anúlela antes de registrar otra"),
    NO_STAFF_TO_CONFIRM(HttpStatus.CONFLICT,
            "No se puede confirmar el personal del %s: no queda ningún vendedor trabajando"),
    NOVELTY_NOT_FOUND(HttpStatus.NOT_FOUND, "No se encontró la novedad con id %s"),

    // Automatic rotation
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "El mes %s ya pasó; solo se puede generar el mes en curso o uno futuro"),
    INVALID_PHASE(HttpStatus.BAD_REQUEST, "La fase %s no es válida; debe estar entre 0 y 5"),
    INVALID_RANGE(HttpStatus.BAD_REQUEST, "%s"),
    ROTATION_CODE_INACTIVE(HttpStatus.CONFLICT,
            "El código de turno %s que necesita la rotación no existe o está inactivo"),
    PHASE_REQUIRED(HttpStatus.CONFLICT, "No se pudo deducir la fase de %s; elíjala (la más probable es %s)"),
    SCHEDULE_CHANGED(HttpStatus.CONFLICT,
            "La malla cambió desde la vista previa; repita la vista previa antes de confirmar");

    private final HttpStatus status;
    private final String messageTemplate;

    ShiftError(HttpStatus status, String messageTemplate) {
        this.status = status;
        this.messageTemplate = messageTemplate;
    }

    /** HTTP status of the answer. */
    public HttpStatus status() {
        return status;
    }

    /** Message for the end user with the placeholders filled in, in order. */
    public String message(Object... args) {
        return messageTemplate.formatted(args);
    }
}
