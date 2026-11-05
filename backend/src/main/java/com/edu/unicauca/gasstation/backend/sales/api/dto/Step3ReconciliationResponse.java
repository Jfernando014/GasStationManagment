package com.edu.unicauca.gasstation.backend.sales.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for Step 3 reconciliation (Credibanco commission check).
 *
 * @param journalId          ID del journal
 * @param journalDate        Fecha operacional del journal
 * @param totalCredibanco    Total Credibanco declarado en el paso 2
 * @param commissionRate     Tasa de comisión Credibanco vigente (ej. 0.0265 = 2.65%)
 * @param expectedCommission Comisión calculada = totalCredibanco × commissionRate
 * @param totalDelivered     Total entregado declarado en el paso 2
 * @param reconciles         {@code true} si el paso 3 cuadra y puede marcarse como completo
 * @param stepCompleted      {@code true} si el paso 3 fue marcado como completo en esta llamada
 */
public record Step3ReconciliationResponse(
        Long journalId,
        LocalDate journalDate,
        BigDecimal totalCredibanco,
        BigDecimal commissionRate,
        BigDecimal expectedCommission,
        BigDecimal totalDelivered,
        boolean reconciles,
        boolean stepCompleted
) {}
