package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.AdditiveIncentiveResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.CreateRuleRequest;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.CreateRuleResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.RuleHistoryResponse;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/** Swagger documentation of the incentive rule endpoints. */
@Tag(name = "Incentive rules", description = "Reglas de incentivo por aditivo: pesos que gana el vendedor por cada "
        + "unidad vendida desde una fecha de vigencia. El historial no se edita ni se borra; para cambiar un valor "
        + "se crea una regla nueva, y para dejar de pagar incentivo se crea una de $0 desde la fecha de corte.")
public interface IncentiveRuleApi {

    @Operation(summary = "Listar los aditivos con su incentivo vigente",
            description = "Una fila por aditivo activo del catálogo, ordenada por nombre: el valor que aplica en la "
                    + "fecha, desde cuándo, y el próximo valor ya programado. Los aditivos que nunca han tenido "
                    + "regla vienen marcados como \"Sin incentivo\".")
    @ApiResponse(responseCode = "200", description = "Aditivos con su incentivo")
    @ApiResponse(responseCode = "400", description = "Fecha con formato inválido",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    List<AdditiveIncentiveResponse> list(LocalDate date);

    @Operation(summary = "Historial de reglas de un aditivo",
            description = "Todas las reglas del aditivo, de la más reciente a la más antigua, cada una con su estado "
                    + "a hoy: PROGRAMADO (vigencia futura), VIGENTE (la que aplica hoy) o ANTERIOR. Un aditivo "
                    + "inactivo conserva su historial.")
    @ApiResponse(responseCode = "200", description = "Historial del aditivo")
    @ApiResponse(responseCode = "400", description = "El producto no es un aditivo",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe el producto",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    List<RuleHistoryResponse> history(Long productId);

    @Operation(summary = "Registrar una regla de incentivo",
            description = "Solo ADMIN. Crea una regla nueva para el aditivo desde la fecha de vigencia. Si el valor "
                    + "es igual al que ya aplica en esa fecha no se crea nada y la respuesta lo indica "
                    + "(created = false). Las advertencias (valor mayor al 50 % del precio, vigencia que no es día 1 "
                    + "del mes) no impiden crear la regla.")
    @ApiResponse(responseCode = "201", description = "Regla creada, con sus advertencias si las hay")
    @ApiResponse(responseCode = "200", description = "No se creó porque el valor ya era el que aplica en esa fecha")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, valor fuera de 0 a "
            + IncentiveRule.MAX_VALUE_PER_UNIT + " o producto que no es aditivo",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "404", description = "No existe el producto",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Aditivo inactivo, vigencia no posterior a la última jornada "
            + "cerrada, o ya existe una regla con esa vigencia",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<CreateRuleResponse> create(CreateRuleRequest request, Authentication authentication);
}
