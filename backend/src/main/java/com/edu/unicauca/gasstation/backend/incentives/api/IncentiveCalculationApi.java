package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveCalculationDtos.MonthlyIncentivesResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveCalculationDtos.VendorMonthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/** Swagger documentation of the monthly incentive calculation endpoints. */
@Tag(name = "Incentive calculation", description = "Incentivo mensual de cada vendedor: unidades de cada aditivo "
        + "vendidas en el mes por el valor por unidad de la regla vigente el día de cada venta. Si una regla cambia a "
        + "mitad de mes, cada día usa la suya. Además se informa el total acumulado desde el primer mes configurado "
        + "(noviembre de 2025), que no se reinicia. Las unidades de un SKU sin producto o sin regla vigente no suman "
        + "y se reportan aparte. Solo consulta.")
public interface IncentiveCalculationApi {

    @Operation(summary = "Calcular el incentivo de un mes para todos los vendedores",
            description = "Un elemento por vendedor que vendió aditivos en el mes, ordenado por nombre. Un mes sin "
                    + "ventas devuelve la lista de vendedores vacía. El mes en curso se calcula con lo vendido hasta "
                    + "hoy.")
    @ApiResponse(responseCode = "200", description = "Incentivos del mes")
    @ApiResponse(responseCode = "400", description = "Mes con formato inválido, futuro o anterior al inicio del "
            + "cálculo",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    MonthlyIncentivesResponse calculateMonth(YearMonth month);

    @Operation(summary = "Calcular el incentivo de un mes para un vendedor",
            description = "El vendedor se identifica por su cédula, como la registra Ventas. Si no vendió aditivos en "
                    + "el mes, igual se informa su acumulado de los meses anteriores; si nunca vendió, se devuelven "
                    + "ceros y listas vacías.")
    @ApiResponse(responseCode = "200", description = "Incentivo del vendedor en el mes")
    @ApiResponse(responseCode = "400", description = "Mes con formato inválido, futuro o anterior al inicio del "
            + "cálculo",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    VendorMonthResponse calculateVendor(YearMonth month, String vendorDocument);
}
