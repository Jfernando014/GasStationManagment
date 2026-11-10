package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.api.AdditiveSalesDtos.AdditiveSalesDayResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.AdditiveSalesDtos.SyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;

/** Swagger documentation of the additive sales endpoints. */
@Tag(name = "Additive sales", description = "Unidades de aditivos vendidas por cada vendedor en cada jornada cerrada. "
        + "Se guardan solas al cerrar la jornada; si el guardado falla, la fecha queda pendiente hasta que un "
        + "administrador la reprocese. Solo son unidades: los importes de incentivo se calculan aparte.")
public interface AdditiveSalesApi {

    @Operation(summary = "Consultar las ventas de aditivos de una fecha",
            description = "Estado de la fecha (PROCESSED o PENDING) y las unidades por vendedor y aditivo. Una fecha "
                    + "procesada sin ventas de aditivos devuelve la lista vacía. Si la fecha tiene un fallo, las "
                    + "filas son las de la última vez que se guardó con éxito.")
    @ApiResponse(responseCode = "200", description = "Estado y unidades de la fecha")
    @ApiResponse(responseCode = "400", description = "Fecha con formato inválido",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "La fecha no tiene registro de procesamiento",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    AdditiveSalesDayResponse getDay(LocalDate date);

    @Operation(summary = "Listar las fechas pendientes",
            description = "Fechas cuyo guardado de ventas de aditivos falló, de la más antigua a la más reciente, "
                    + "con el último error y el número de intentos. Se resuelven reprocesándolas.")
    @ApiResponse(responseCode = "200", description = "Fechas pendientes; vacío si no hay ninguna")
    List<SyncResponse> listPending();

    @Operation(summary = "Reprocesar las ventas de aditivos de una fecha",
            description = "Solo ADMIN. Solo para fechas que ya tienen registro (PROCESSED o PENDING), es decir, "
                    + "cuya jornada ya se cerró. Vuelve a leer los cierres de vendedores de Ventas y reemplaza las "
                    + "unidades de la fecha con los datos actuales, igual que al cerrar la jornada: si no hay "
                    + "ventas de aditivos, queda procesada sin filas. Repetirlo deja el mismo resultado. Responde "
                    + "lo mismo que la consulta de la fecha. Si falla, la fecha queda PENDING con el motivo.")
    @ApiResponse(responseCode = "200", description = "Fecha procesada; estado y unidades guardadas")
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
    AdditiveSalesDayResponse reprocess(LocalDate date, Authentication authentication);
}
