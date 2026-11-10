package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSalesDay;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Responses of the additive sales API. */
public final class AdditiveSalesDtos {

    private AdditiveSalesDtos() {
    }

    @Schema(description = "Estado del procesamiento de las ventas de aditivos de una fecha")
    public record SyncResponse(
            @Schema(description = "Fecha operativa de la jornada", example = "2026-02-10")
            LocalDate date,

            @Schema(description = "PROCESSED (unidades guardadas) o PENDING (falló y debe reprocesarse)",
                    example = "PROCESSED")
            SyncStatus status,

            @Schema(description = "Id de la jornada cerrada; nulo si la fecha se procesó a mano sin un cierre previo",
                    example = "42", nullable = true)
            Long journalId,

            @Schema(description = "Número de planilla de la jornada; nulo si no se conoce", example = "42",
                    nullable = true)
            Long consecutive,

            @Schema(description = "Hora en que se cerró la jornada; nula si no se conoce",
                    example = "2026-02-10T21:35:10", nullable = true)
            LocalDateTime closedAt,

            @Schema(description = "Veces que se ha intentado procesar la fecha", example = "1")
            int attempts,

            @Schema(description = "Motivo del último fallo; nulo si la fecha está procesada", nullable = true)
            String lastError,

            @Schema(description = "Última vez que se guardó la fecha con éxito; nulo si nunca se logró",
                    example = "2026-02-10T21:35:11Z", nullable = true)
            Instant processedAt,

            @Schema(description = "Última vez que cambió el estado", example = "2026-02-10T21:35:11Z")
            Instant updatedAt) {

        static SyncResponse from(JournalSync sync) {
            return new SyncResponse(sync.journalDate(), sync.status(), sync.journalId(), sync.consecutive(),
                    sync.closedAt(), sync.attempts(), sync.lastError(), sync.processedAt(), sync.updatedAt());
        }
    }

    @Schema(description = "Unidades de un aditivo vendidas por un vendedor en la jornada")
    public record AdditiveSaleResponse(
            @Schema(description = "Cédula del vendedor, como la registra Ventas", example = "1000000001")
            String vendorDocument,

            @Schema(description = "Nombre del vendedor", example = "Vendedor de prueba Uno")
            String vendorName,

            @Schema(description = "SKU del aditivo, el código del producto en Inventario", example = "ADT-001")
            String sku,

            @Schema(description = "Nombre del aditivo", example = "Aditivo de prueba Alfa")
            String productName,

            @Schema(description = "Unidades vendidas, mayor que cero", example = "3")
            int units) {

        static AdditiveSaleResponse from(AdditiveSaleUnits sale) {
            return new AdditiveSaleResponse(sale.vendorDocument(), sale.vendorName(), sale.sku(), sale.productName(),
                    sale.units());
        }
    }

    @Schema(description = "Estado de una fecha y las unidades de aditivos guardadas para ella")
    public record AdditiveSalesDayResponse(
            @Schema(description = "Estado del procesamiento de la fecha")
            SyncResponse sync,

            @Schema(description = "Unidades por vendedor y aditivo, ordenadas por nombre del vendedor y SKU; vacío si "
                    + "no hubo ventas de aditivos")
            List<AdditiveSaleResponse> sales) {

        static AdditiveSalesDayResponse from(AdditiveSalesDay day) {
            return new AdditiveSalesDayResponse(SyncResponse.from(day.sync()),
                    day.sales().stream().map(AdditiveSaleResponse::from).toList());
        }
    }
}
