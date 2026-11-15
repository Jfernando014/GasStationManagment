package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.SheetResponse;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.SummaryResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.services.OvertimeSheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Fortnight overtime sheets: read only, they need a session. Nothing is stored, each sheet is worked out when asked. */
@Tag(name = "Overtime sheet", description = "Sábana quincenal de horas extra: por cada día de la quincena, las "
        + "horas guardadas clasificadas en recargos (RDF diurnas de domingo o festivo, RNO nocturnas ordinarias, RNF "
        + "nocturnas de domingo o festivo), con subtotales, la línea de HEDO fijas, los ajustes manuales y el total a "
        + "pagar. Se calcula al consultar con los parámetros vigentes cada día; un día sin registro, pendiente o "
        + "guardado sin novedades se informa en la respuesta, nunca se oculta.")
@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class OvertimeSheetController {

    private final OvertimeSheetService service;

    @Operation(summary = "Sábana de un trabajador en una quincena",
            description = "Todos los días de la quincena con fecha, día de la semana, si es festivo (domingo o día "
                    + "festivo), código trabajado, horas brutas y las horas de cada categoría; subtotales, horas "
                    + "brutas totales, línea de HEDO fijas si el rol la tiene encendida, ajustes y total a pagar por "
                    + "categoría. Incluye las fechas incompletas o sin novedades confirmadas.")
    @ApiResponse(responseCode = "200", description = "Sábana de la quincena")
    @ApiResponse(responseCode = "400", description = "Mes con formato inválido o quincena distinta de 1 y 2",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe el trabajador",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{workerId}/overtime-sheet")
    public SheetResponse getWorkerSheet(
            @Parameter(description = "Id del trabajador") @PathVariable UUID workerId,
            @Parameter(description = "Mes (yyyy-MM)", example = "2026-08")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(description = "Quincena: 1 (días 1 a 15) o 2 (día 16 al último)", example = "1")
            @RequestParam int fortnight) {
        return SheetResponse.from(service.getWorkerSheet(workerId, month, fortnight));
    }

    @Operation(summary = "Resumen de todos los trabajadores en una quincena",
            description = "Una fila por trabajador con horas o ajustes en la quincena, ordenada por nombre: horas "
                    + "brutas, subtotales, HEDO fijas, ajustes y total a pagar por categoría, con la bandera de "
                    + "incompleta y la lista de fechas afectadas.")
    @ApiResponse(responseCode = "200", description = "Resumen de la quincena; sin trabajadores si no hay horas")
    @ApiResponse(responseCode = "400", description = "Mes con formato inválido o quincena distinta de 1 y 2",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/overtime-sheet")
    public SummaryResponse getSummary(
            @Parameter(description = "Mes (yyyy-MM)", example = "2026-08")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(description = "Quincena: 1 (días 1 a 15) o 2 (día 16 al último)", example = "1")
            @RequestParam int fortnight) {
        return SummaryResponse.from(service.getSummary(month, fortnight));
    }
}
