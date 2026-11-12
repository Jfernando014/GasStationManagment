package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursSegment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import com.edu.unicauca.gasstation.backend.workers.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerHoursRange;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Responses of the worked hours API. */
public final class WorkerHoursDtos {

    private WorkerHoursDtos() {
    }

    @Schema(description = "Bloque de horas trabajadas. Si la hora de fin es menor que la de inicio, termina al día "
            + "siguiente")
    public record SegmentResponse(
            @Schema(description = "Hora de inicio (0 a 23)", example = "6")
            int startHour,

            @Schema(description = "Hora de fin (1 a 24)", example = "18")
            int endHour,

            @Schema(description = "Horas del bloque", example = "12")
            int hours) {

        static SegmentResponse from(DayHoursSegment segment) {
            return new SegmentResponse(segment.startHour(), segment.endHour(), segment.hours());
        }
    }

    @Schema(description = "Horas de un trabajador en un día cerrado, tal como las entregó Turnos. Es una copia fija "
            + "del día: no descuenta descansos ni calcula recargos")
    public record WorkerDayHoursResponse(
            @Schema(description = "Fecha operativa del día", example = "2026-03-10")
            LocalDate date,

            @Schema(description = "Id del trabajador", example = "3f1c2b4e-8a6d-4c1f-9b7e-2d5a6c8e0f11")
            UUID workerId,

            @Schema(description = "Nombre del trabajador ese día", example = "Trabajador de prueba Uno")
            String workerName,

            @Schema(description = "Código de turno trabajado; nulo si el trabajador estaba en la malla pero no "
                    + "trabajó", example = "DIA", nullable = true)
            String workedCode,

            @Schema(description = "Código que el trabajador tenía en la malla", example = "DIA")
            String gridCode,

            @Schema(description = "Rol del día, según el código trabajado; nulo si no trabajó", example = "TITULAR",
                    nullable = true)
            String dayRole,

            @Schema(description = "Total de horas trabajadas; 0 si no trabajó", example = "12")
            int totalHours,

            @Schema(description = "Verdadero si estaba en la malla y no trabajó (ausencia o reemplazado)",
                    example = "false")
            boolean absent,

            @Schema(description = "Novedad del día: NONE, REPLACEMENT, ABSENCE, SHIFT_CHANGE o SWAP",
                    example = "NONE")
            String noveltyType,

            @Schema(description = "Trabajador al que reemplazó o con quien cambió; nulo en otro caso",
                    nullable = true)
            UUID replacedWorkerId,

            @Schema(description = "Nombre del trabajador reemplazado; nulo en otro caso", nullable = true)
            String replacedWorkerName,

            @Schema(description = "Motivo de la novedad; nulo si no hay novedad", nullable = true)
            String reason,

            @Schema(description = "Jornada que cerró el día; nulo si no se conoce", example = "42", nullable = true)
            Long journalId,

            @Schema(description = "Falso si el personal del día se leyó antes de poder aplicar sus novedades "
                    + "(paso 1 sin confirmar): las horas son las de la malla, sin reemplazos ni ausentes. Reprocesar "
                    + "la fecha lo actualiza si las novedades llegan a aplicarse", example = "true")
            boolean noveltiesApplied,

            @Schema(description = "Bloques de horas del código trabajado; vacío si no trabajó")
            List<SegmentResponse> segments) {

        static WorkerDayHoursResponse from(WorkerDayHours hours) {
            return new WorkerDayHoursResponse(hours.workDate(), hours.workerId(), hours.workerName(),
                    hours.workedCode(), hours.gridCode(), hours.dayRole(), hours.totalHours(), hours.absent(),
                    hours.noveltyType(), hours.replacedWorkerId(), hours.replacedWorkerName(), hours.reason(),
                    hours.journalId(), hours.noveltiesApplied(),
                    hours.segments().stream().map(SegmentResponse::from).toList());
        }
    }

    @Schema(description = "Estado del procesamiento de las horas de una fecha")
    public record SyncResponse(
            @Schema(description = "Fecha operativa de la jornada", example = "2026-03-10")
            LocalDate date,

            @Schema(description = "PROCESSED (horas guardadas) o PENDING (falló y debe reprocesarse)",
                    example = "PROCESSED")
            SyncStatus status,

            @Schema(description = "Falso si el personal del día se leyó antes de poder aplicar sus novedades",
                    example = "true")
            boolean noveltiesApplied,

            @Schema(description = "Id de la jornada cerrada; nulo si la fecha se procesó a mano sin un cierre previo",
                    example = "42", nullable = true)
            Long journalId,

            @Schema(description = "Número de planilla de la jornada; nulo si no se conoce", example = "42",
                    nullable = true)
            Long consecutive,

            @Schema(description = "Hora en que se cerró la jornada; nula si no se conoce",
                    example = "2026-03-10T21:35:10", nullable = true)
            LocalDateTime closedAt,

            @Schema(description = "Veces que se ha intentado procesar la fecha", example = "1")
            int attempts,

            @Schema(description = "Motivo del último fallo; nulo si la fecha está procesada", nullable = true)
            String lastError,

            @Schema(description = "Última vez que se guardó la fecha con éxito; nulo si nunca se logró",
                    example = "2026-03-10T21:35:11Z", nullable = true)
            Instant processedAt,

            @Schema(description = "Última vez que cambió el estado", example = "2026-03-10T21:35:11Z")
            Instant updatedAt) {

        public static SyncResponse from(HoursSync sync) {
            return new SyncResponse(sync.workDate(), sync.status(), sync.noveltiesApplied(), sync.journalId(),
                    sync.consecutive(), sync.closedAt(), sync.attempts(), sync.lastError(), sync.processedAt(),
                    sync.updatedAt());
        }
    }

    @Schema(description = "Estado de una fecha y las horas de cada trabajador ese día")
    public record DayHoursResponse(
            @Schema(description = "Estado del procesamiento de la fecha")
            SyncResponse sync,

            @Schema(description = "Un elemento por trabajador que trabajó o estaba en la malla y no trabajó, "
                    + "ordenado por nombre; vacío si el día no tenía personal")
            List<WorkerDayHoursResponse> workers) {

        public static DayHoursResponse from(DayHours day) {
            return new DayHoursResponse(SyncResponse.from(day.sync()),
                    day.workers().stream().map(WorkerDayHoursResponse::from).toList());
        }
    }

    @Schema(description = "Horas de un trabajador en un rango de fechas")
    public record WorkerHoursRangeResponse(
            @Schema(description = "Id del trabajador", example = "3f1c2b4e-8a6d-4c1f-9b7e-2d5a6c8e0f11")
            UUID workerId,

            @Schema(description = "Nombre actual del trabajador", example = "Trabajador de prueba Uno")
            String workerName,

            @Schema(description = "Primer día del rango", example = "2026-03-01")
            LocalDate from,

            @Schema(description = "Último día del rango", example = "2026-03-14")
            LocalDate to,

            @Schema(description = "Suma de las horas de los días guardados del rango", example = "96")
            int totalHours,

            @Schema(description = "Días guardados del rango en orden de fecha; los días sin registro no aparecen")
            List<WorkerDayHoursResponse> days,

            @Schema(description = "Fechas del rango cuyo procesamiento falló y por eso faltan en el total")
            List<LocalDate> pendingDates) {

        public static WorkerHoursRangeResponse from(WorkerHoursRange range) {
            return new WorkerHoursRangeResponse(range.workerId(), range.workerName(), range.from(), range.to(),
                    range.totalHours(), range.days().stream().map(WorkerDayHoursResponse::from).toList(),
                    range.pendingDates());
        }
    }
}
