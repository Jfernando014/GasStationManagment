package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.CreateParametersRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.ParameterOverviewResponse;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.ParameterVersionResponse;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.ParametersResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.services.OvertimeParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Overtime parameters: reading needs a session, creating a version needs ADMIN. Versions are not edited or deleted. */
@Tag(name = "Overtime parameters", description = "Parámetros de la sábana de horas extra: hora en que empieza la "
        + "noche, horas de descanso y mínimo de horas del turno para descontarlo, horas de HEDO fijas y el "
        + "interruptor de esas horas por rol (apagado por defecto). Cada cambio crea una versión con fecha de "
        + "vigencia; el valor de un día es el de la versión vigente ese día y las versiones no se editan.")
@RestController
@RequestMapping("/api/v1/workers/overtime-parameters")
@RequiredArgsConstructor
public class OvertimeParameterController {

    private final OvertimeParameterService service;

    @Operation(summary = "Consultar los parámetros",
            description = "La versión vigente en la fecha, las versiones ya programadas después de ella y el "
                    + "historial completo.")
    @ApiResponse(responseCode = "200", description = "Parámetros vigentes, programados e historial")
    @ApiResponse(responseCode = "400", description = "Fecha con formato inválido",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public ParameterOverviewResponse getOverview(
            @Parameter(description = "Fecha a consultar (yyyy-MM-dd); por defecto hoy en Bogotá", example = "2026-08-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ParameterOverviewResponse.from(service.overview(date));
    }

    @Operation(summary = "Historial de versiones de los parámetros",
            description = "Todas las versiones, de la más reciente a la más antigua. Cada una trae su primer día de "
                    + "vigencia, el último día en que aplica (el día anterior al inicio de la siguiente; nulo si no "
                    + "hay una posterior), sus valores, los roles con HEDO fijas encendidas, el motivo si se dio, "
                    + "quién la creó y cuándo. Marca la que aplica hoy.")
    @ApiResponse(responseCode = "200", description = "Versiones de los parámetros")
    @GetMapping("/history")
    public List<ParameterVersionResponse> getHistory() {
        return service.history().stream().map(ParameterVersionResponse::from).toList();
    }

    @Operation(summary = "Crear una versión de los parámetros",
            description = "Solo ADMIN. Crea una versión nueva con todos los valores desde la fecha de vigencia, que "
                    + "debe ser posterior a la última fecha con horas registradas: los días ya usados conservan sus "
                    + "valores. Los roles de fixedHedoRoles tienen encendida la línea de HEDO fijas; los demás, "
                    + "apagada. El motivo es opcional.")
    @ApiResponse(responseCode = "201", description = "Versión creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, valor fuera de rango, rol desconocido o motivo "
            + "de menos de 3 o más de 255 caracteres",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "409", description = "Vigencia no posterior a la última fecha con horas registradas, "
            + "o ya existe una versión con esa vigencia",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParametersResponse create(@Valid @RequestBody CreateParametersRequest request,
                                     Authentication authentication) {
        WorkersAdminAuthorization.requireAdmin(authentication);
        return ParametersResponse.from(service.create(request.validFrom(), request.nightStartHour(),
                request.restHours(), request.restMinShiftHours(), request.fixedHedoHours(),
                request.fixedHedoRoles(), request.reason(), authentication.getName()));
    }
}
