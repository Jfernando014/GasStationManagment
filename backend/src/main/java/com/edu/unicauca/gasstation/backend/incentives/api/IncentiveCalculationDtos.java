package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveLine;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.MonthlyIncentives;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.UnresolvedAdditive;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.UnresolvedReason;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.VendorIncentive;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** Responses of the monthly incentive calculation API. */
public final class IncentiveCalculationDtos {

    private IncentiveCalculationDtos() {
    }

    @Schema(description = "Unidades de un aditivo que ganaron el mismo valor por unidad. Si la regla cambió durante "
            + "el mes, el aditivo aparece una vez por cada valor")
    public record LineResponse(
            @Schema(description = "SKU del aditivo, el código del producto en Inventario", example = "ADT-001")
            String sku,

            @Schema(description = "Nombre del aditivo", example = "Aditivo de prueba Alfa")
            String productName,

            @Schema(description = "Unidades vendidas en el mes con ese valor", example = "5")
            int units,

            @Schema(description = "Pesos por unidad aplicados, según la regla vigente el día de cada venta",
                    example = "1000")
            BigDecimal valuePerUnit,

            @Schema(description = "Unidades por valor por unidad, en pesos", example = "5000")
            BigDecimal subtotal) {

        static LineResponse from(IncentiveLine line) {
            return new LineResponse(line.sku(), line.productName(), line.units(), line.valuePerUnit(),
                    line.subtotal());
        }
    }

    @Schema(description = "Unidades que no se pudieron convertir en pesos; no suman a ningún total")
    public record UnresolvedResponse(
            @Schema(description = "SKU reportado por Ventas", example = "ADT-009")
            String sku,

            @Schema(description = "Nombre del producto según Ventas", example = "Aditivo sin regla")
            String productName,

            @Schema(description = "Unidades vendidas en el mes que quedaron sin valor", example = "2")
            int units,

            @Schema(description = "PRODUCT_NOT_FOUND (el SKU no existe en el catálogo) o NO_RULE_IN_FORCE (el aditivo "
                    + "no tenía regla vigente el día de la venta)", example = "NO_RULE_IN_FORCE")
            UnresolvedReason reason,

            @Schema(description = "Motivo en palabras para el usuario")
            String message) {

        static UnresolvedResponse from(UnresolvedAdditive item) {
            return new UnresolvedResponse(item.sku(), item.productName(), item.units(), item.reason(),
                    item.reason().message());
        }
    }

    @Schema(description = "Incentivo de un vendedor en el mes")
    public record VendorIncentiveResponse(
            @Schema(description = "Cédula del vendedor, como la registra Ventas", example = "1000000001")
            String vendorDocument,

            @Schema(description = "Nombre del vendedor; nulo si no tiene ninguna venta de aditivos desde el inicio "
                    + "del acumulado", example = "Vendedor de prueba Uno", nullable = true)
            String vendorName,

            @Schema(description = "Aditivos vendidos en el mes con su valor por unidad y subtotal")
            List<LineResponse> lines,

            @Schema(description = "Aditivos del mes que no se pudieron valorar y no están en los totales")
            List<UnresolvedResponse> unresolved,

            @Schema(description = "Suma de los subtotales del mes, en pesos", example = "9000")
            BigDecimal monthTotal,

            @Schema(description = "Suma de los meses desde el inicio del acumulado hasta el mes consultado, en pesos",
                    example = "15000")
            BigDecimal accumulatedTotal,

            @Schema(description = "Unidades vendidas desde el inicio del acumulado hasta el mes consultado, este "
                    + "incluido, que no se pudieron valorar y por eso faltan en el total acumulado. Si es 0, el "
                    + "acumulado está completo. La lista de no resueltos solo trae las del mes", example = "3")
            int accumulatedUnresolvedUnits) {

        static VendorIncentiveResponse from(VendorIncentive vendor) {
            return new VendorIncentiveResponse(vendor.vendorDocument(), vendor.vendorName(),
                    vendor.lines().stream().map(LineResponse::from).toList(),
                    vendor.unresolved().stream().map(UnresolvedResponse::from).toList(),
                    vendor.monthTotal(), vendor.accumulatedTotal(), vendor.accumulatedUnresolvedUnits());
        }
    }

    @Schema(description = "Incentivos del mes de todos los vendedores que vendieron aditivos")
    public record MonthlyIncentivesResponse(
            @Schema(description = "Mes calculado (yyyy-MM)", example = "2026-03")
            String month,

            @Schema(description = "Primer mes del total acumulado (yyyy-MM)", example = "2025-11")
            String accumulatedFrom,

            @Schema(description = "Un elemento por vendedor con ventas de aditivos en el mes, ordenados por nombre; "
                    + "vacío si no hubo ventas")
            List<VendorIncentiveResponse> vendors) {

        static MonthlyIncentivesResponse from(MonthlyIncentives result) {
            return new MonthlyIncentivesResponse(result.month().toString(), result.accumulatedFrom().toString(),
                    result.vendors().stream().map(VendorIncentiveResponse::from).toList());
        }
    }

    @Schema(description = "Incentivo de un vendedor en el mes")
    public record VendorMonthResponse(
            @Schema(description = "Mes calculado (yyyy-MM)", example = "2026-03")
            String month,

            @Schema(description = "Primer mes del total acumulado (yyyy-MM)", example = "2025-11")
            String accumulatedFrom,

            @Schema(description = "Incentivo del vendedor; con ceros y listas vacías si no tiene ventas")
            VendorIncentiveResponse vendor) {

        static VendorMonthResponse from(MonthlyIncentives result) {
            return new VendorMonthResponse(result.month().toString(), result.accumulatedFrom().toString(),
                    VendorIncentiveResponse.from(result.vendors().getFirst()));
        }
    }
}
