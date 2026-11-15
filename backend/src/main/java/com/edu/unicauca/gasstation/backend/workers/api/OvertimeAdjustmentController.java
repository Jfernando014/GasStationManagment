package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.AdjustmentResponse;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.OvertimeDtos.CreateAdjustmentRequest;
import com.edu.unicauca.gasstation.backend.workers.domain.services.OvertimeAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Manual overtime adjustments: reading needs a session, creating and deleting need ADMIN. */
@Tag(name = "Overtime adjustments", description = "Ajustes manuales de la sábana de horas extra: por trabajador, "
        + "quincena y categoría, el administrador suma (ADD) o resta (SUBTRACT) horas con un motivo corto. El total a "
        + "pagar de la categoría es subtotal + más − menos. No hay arrastre automático entre quincenas.")
@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class OvertimeAdjustmentController {

    private final OvertimeAdjustmentService service;

    @Operation(summary = "Crear un ajuste",
            description = "Solo ADMIN. Registra horas que suman (ADD) o restan (SUBTRACT) a una categoría de la "
                    + "quincena del trabajador. Las horas deben ser mayores que cero y el motivo, de 3 a 255 "
                    + "caracteres.")
    @ApiResponse(responseCode = "201", description = "Ajuste creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, quincena distinta de 1 y 2, horas en cero o "
            + "negativas, o categoría desconocida",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "404", description = "No existe el trabajador",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{workerId}/overtime-adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjustmentResponse create(@Parameter(description = "Id del trabajador") @PathVariable UUID workerId,
                                     @Valid @RequestBody CreateAdjustmentRequest request,
                                     Authentication authentication) {
        WorkersAdminAuthorization.requireAdmin(authentication);
        return AdjustmentResponse.from(service.create(workerId, request.month(), request.fortnight(),
                request.category(), request.direction(), request.hours(), request.reason(),
                authentication.getName()));
    }

    @Operation(summary = "Listar los ajustes de un trabajador en una quincena",
            description = "Del más antiguo al más reciente.")
    @ApiResponse(responseCode = "200", description = "Ajustes de la quincena; vacío si no hay")
    @ApiResponse(responseCode = "400", description = "Mes con formato inválido o quincena distinta de 1 y 2",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe el trabajador",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{workerId}/overtime-adjustments")
    public List<AdjustmentResponse> list(
            @Parameter(description = "Id del trabajador") @PathVariable UUID workerId,
            @Parameter(description = "Mes (yyyy-MM)", example = "2026-08")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(description = "Quincena: 1 (días 1 a 15) o 2 (día 16 al último)", example = "1")
            @RequestParam int fortnight) {
        return service.list(workerId, month, fortnight).stream().map(AdjustmentResponse::from).toList();
    }

    @Operation(summary = "Eliminar un ajuste", description = "Solo ADMIN. El ajuste deja de contar en la sábana.")
    @ApiResponse(responseCode = "204", description = "Ajuste eliminado")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "404", description = "No existe el ajuste",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/overtime-adjustments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id del ajuste") @PathVariable Long id,
                       Authentication authentication) {
        WorkersAdminAuthorization.requireAdmin(authentication);
        service.delete(id);
    }
}
