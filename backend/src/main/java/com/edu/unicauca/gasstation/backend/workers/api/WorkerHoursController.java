package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerHoursDtos.DayHoursResponse;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerHoursDtos.SyncResponse;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerHoursDtos.WorkerHoursRangeResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.services.WorkerHoursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Worked hours endpoints: reading needs a session, reprocessing a date needs ADMIN. The hours are kept by the
 * listener of closed journals; nothing here creates them except the reprocess.
 */
@Tag(name = "Worker hours", description = "Horas reales trabajadas por cada trabajador en cada día cerrado. Se "
        + "guardan solas al cerrar la jornada, como una copia fija de lo que entregó Turnos ese día (reemplazos y "
        + "ausencias incluidos). Son las horas del código trabajado, sin descontar descansos ni calcular recargos. "
        + "Si el guardado falla, la fecha queda pendiente hasta que un administrador la reprocese.")
@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerHoursController {

    private final WorkerHoursService service;

    @Operation(summary = "Horas de un trabajador en un rango de fechas",
            description = "Los días guardados del trabajador entre las dos fechas (ambas incluidas), en orden de "
                    + "fecha, con el total de horas. Un día en el que estaba en la malla y no trabajó aparece con 0 "
                    + "horas y su novedad. Las fechas del rango que están pendientes de procesar se listan aparte "
                    + "porque no suman al total. El rango no puede superar " + DayStaffQuery.MAX_RANGE_DAYS + " días.")
    @ApiResponse(responseCode = "200", description = "Horas del trabajador en el rango")
    @ApiResponse(responseCode = "400", description = "Fechas con formato inválido, rango invertido o de más de "
            + DayStaffQuery.MAX_RANGE_DAYS + " días",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe el trabajador",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{workerId}/hours")
    public WorkerHoursRangeResponse getWorkerHours(
            @Parameter(description = "Id del trabajador") @PathVariable UUID workerId,
            @Parameter(description = "Primer día del rango (yyyy-MM-dd)", example = "2026-03-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Último día del rango (yyyy-MM-dd)", example = "2026-03-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return WorkerHoursRangeResponse.from(service.getWorkerHours(workerId, from, to));
    }

    @Operation(summary = "Horas de todos los trabajadores en un día",
            description = "Estado de la fecha (PROCESSED o PENDING) y las horas de cada trabajador ese día. Si la "
                    + "fecha tiene un fallo, las filas son las de la última vez que se guardó con éxito.")
    @ApiResponse(responseCode = "200", description = "Estado y horas de la fecha")
    @ApiResponse(responseCode = "400", description = "Fecha con formato inválido",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "La fecha no tiene registro de procesamiento",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/hours/{date}")
    public DayHoursResponse getDay(
            @Parameter(description = "Fecha operativa de la jornada (yyyy-MM-dd)", example = "2026-03-10")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DayHoursResponse.from(service.getDay(date));
    }

    @Operation(summary = "Listar las fechas pendientes",
            description = "Fechas cuyo guardado de horas falló, de la más antigua a la más reciente, con el último "
                    + "error y el número de intentos. Se resuelven reprocesándolas.")
    @ApiResponse(responseCode = "200", description = "Fechas pendientes; vacío si no hay ninguna")
    @GetMapping("/hours/pending")
    public List<SyncResponse> listPending() {
        return service.listPending().stream().map(SyncResponse::from).toList();
    }

    @Operation(summary = "Reprocesar las horas de una fecha",
            description = "Solo ADMIN. Solo para fechas que ya tienen registro (PROCESSED o PENDING), es decir, "
                    + "cuya jornada ya se cerró. Vuelve a leer el personal del día en Turnos y reemplaza lo guardado "
                    + "de esa fecha; repetirlo deja el mismo resultado. Turnos no incluye a los trabajadores "
                    + "inactivos, así que sus filas ya guardadas se conservan tal como estaban; las de un trabajador "
                    + "activo que ya no aparece sí se eliminan. Si el día se había "
                    + "guardado sin novedades aplicadas y ahora ya se aplicaron, actualiza las horas y la marca. "
                    + "Responde lo mismo que la consulta del día. Si falla, la fecha queda PENDING con el motivo.")
    @ApiResponse(responseCode = "200", description = "Fecha procesada; estado y horas guardadas")
    @ApiResponse(responseCode = "400", description = "Fecha con formato inválido",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "404", description = "La fecha no tiene registro de procesamiento",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "500", description = "El procesamiento falló; la fecha quedó pendiente",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/hours/{date}/reprocess")
    public DayHoursResponse reprocess(
            @Parameter(description = "Fecha operativa de la jornada (yyyy-MM-dd)", example = "2026-03-10")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        WorkersAdminAuthorization.requireAdmin(authentication);
        return DayHoursResponse.from(service.reprocess(date));
    }
}
