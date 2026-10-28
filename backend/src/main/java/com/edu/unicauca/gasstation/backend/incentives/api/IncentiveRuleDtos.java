package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveIncentive;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.CreateRuleResult;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleHistoryEntry;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleStatus;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleWarning;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the incentive rules API. */
public final class IncentiveRuleDtos {

    static final String NO_INCENTIVE_LABEL = "Sin incentivo";

    private IncentiveRuleDtos() {
    }

    @Schema(description = "Aditivo activo con el incentivo que aplica en la fecha consultada")
    public record AdditiveIncentiveResponse(
            @Schema(description = "Id del aditivo", example = "9001")
            Long productId,

            @Schema(description = "Nombre del aditivo", example = "Aditivo de prueba Alfa")
            String productName,

            @Schema(description = "Pesos por unidad vendida que aplican en la fecha; nulo si todavía no aplica ninguno",
                    example = "1000", nullable = true)
            Integer valuePerUnit,

            @Schema(description = "Primer día de vigencia del valor que aplica; nulo si no aplica ninguno",
                    example = "2026-02-01", nullable = true)
            LocalDate validFrom,

            @Schema(description = "Valor ya programado para después de la fecha consultada; nulo si no hay",
                    example = "1200", nullable = true)
            Integer nextValuePerUnit,

            @Schema(description = "Primer día de vigencia del valor programado; nulo si no hay",
                    example = "2026-12-01", nullable = true)
            LocalDate nextValidFrom,

            @Schema(description = "Verdadero si el aditivo nunca ha tenido una regla de incentivo", example = "false")
            boolean noIncentive,

            @Schema(description = "\"Sin incentivo\" cuando el aditivo nunca ha tenido una regla; nulo en otro caso",
                    nullable = true)
            String label) {

        static AdditiveIncentiveResponse from(AdditiveIncentive row) {
            IncentiveRule current = row.current();
            IncentiveRule next = row.next();
            return new AdditiveIncentiveResponse(row.productId(), row.productName(),
                    current == null ? null : current.valuePerUnit(),
                    current == null ? null : current.validFrom(),
                    next == null ? null : next.valuePerUnit(),
                    next == null ? null : next.validFrom(),
                    !row.hasRules(),
                    row.hasRules() ? null : NO_INCENTIVE_LABEL);
        }
    }

    // ---- History of one additive

    @Schema(description = "Regla del historial de un aditivo con su estado a hoy")
    public record RuleHistoryResponse(
            @Schema(description = "Id de la regla", example = "15")
            Long id,

            @Schema(description = "Id del aditivo", example = "9001")
            Long productId,

            @Schema(description = "Pesos por unidad vendida", example = "1000")
            int valuePerUnit,

            @Schema(description = "Primer día de vigencia", example = "2026-02-01")
            LocalDate validFrom,

            @Schema(description = "PROGRAMADO (vigencia futura), VIGENTE (la que aplica hoy) o ANTERIOR",
                    example = "VIGENTE")
            RuleStatus status,

            @Schema(description = "Motivo del cambio; puede ser nulo", example = "Ajuste de febrero", nullable = true)
            String reason,

            @Schema(description = "Momento en que se registró la regla", example = "2026-01-20T15:04:05Z")
            Instant createdAt) {

        static RuleHistoryResponse from(RuleHistoryEntry entry) {
            IncentiveRule rule = entry.rule();
            return new RuleHistoryResponse(rule.id(), rule.productId(), rule.valuePerUnit(), rule.validFrom(),
                    entry.status(), rule.reason(), rule.createdAt());
        }
    }

    // ---- Creation of a rule

    @Schema(description = "Datos de la nueva regla de incentivo")
    public record CreateRuleRequest(
            @Schema(description = "Id del aditivo", example = "9001")
            @NotNull(message = "El producto es obligatorio")
            Long productId,

            @Schema(description = "Pesos enteros por unidad vendida, de 0 a " + IncentiveRule.MAX_VALUE_PER_UNIT
                    + "; 0 deja de pagar incentivo",
                    example = "1000")
            @NotNull(message = "El valor por unidad es obligatorio")
            BigDecimal valuePerUnit,

            @Schema(description = "Primer día en que aplica el valor; debe ser posterior a la última jornada cerrada",
                    example = "2026-12-01")
            @NotNull(message = "La fecha de vigencia es obligatoria")
            LocalDate validFrom,

            @Schema(description = "Motivo del cambio; opcional, máximo 255 caracteres", example = "Ajuste de diciembre")
            @Size(max = 255, message = "El motivo no puede tener más de 255 caracteres")
            String reason) {
    }

    @Schema(description = "Regla de incentivo registrada")
    public record RuleResponse(
            @Schema(description = "Id de la regla", example = "15")
            Long id,

            @Schema(description = "Id del aditivo", example = "9001")
            Long productId,

            @Schema(description = "Pesos por unidad vendida", example = "1000")
            int valuePerUnit,

            @Schema(description = "Primer día de vigencia", example = "2026-12-01")
            LocalDate validFrom,

            @Schema(description = "Siempre verdadero al crear la regla", example = "true")
            boolean active,

            @Schema(description = "Motivo del cambio; puede ser nulo", nullable = true)
            String reason,

            @Schema(description = "Momento en que se registró la regla", example = "2026-10-25T15:04:05Z")
            Instant createdAt) {

        static RuleResponse from(IncentiveRule rule) {
            return new RuleResponse(rule.id(), rule.productId(), rule.valuePerUnit(), rule.validFrom(), rule.active(),
                    rule.reason(), rule.createdAt());
        }
    }

    @Schema(description = "Advertencia que no impide crear la regla")
    public record WarningResponse(
            @Schema(description = "Código estable", example = "VALID_FROM_NOT_FIRST_DAY")
            RuleWarning.Code code,

            @Schema(description = "Mensaje para el usuario")
            String message) {

        static WarningResponse from(RuleWarning warning) {
            return new WarningResponse(warning.code(), warning.message());
        }
    }

    @Schema(description = "Resultado de registrar una regla. Si el valor ya era el que aplica en esa fecha no se "
            + "crea la regla (created = false) y no es un error")
    public record CreateRuleResponse(
            @Schema(description = "Verdadero si se creó una regla", example = "true")
            boolean created,

            @Schema(description = "Mensaje para el usuario", example = "Regla de incentivo creada")
            String message,

            @Schema(description = "Regla creada; nulo si no se creó", nullable = true)
            RuleResponse rule,

            @Schema(description = "Advertencias que no impidieron crear la regla")
            List<WarningResponse> warnings) {

        static CreateRuleResponse from(CreateRuleResult result) {
            return new CreateRuleResponse(result.created(), result.message(),
                    result.rule() == null ? null : RuleResponse.from(result.rule()),
                    result.warnings().stream().map(WarningResponse::from).toList());
        }
    }
}
