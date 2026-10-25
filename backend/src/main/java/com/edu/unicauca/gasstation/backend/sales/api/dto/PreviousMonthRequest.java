package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

@Schema(description = "Opening figures of a month (\"VALORES MES ANTERIOR\"), used while it has no daily rows")
public record PreviousMonthRequest(
        @DecimalMin(value = "0", message = "Mastercard no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Mastercard admite máximo 2 decimales") BigDecimal mastercard,
        @DecimalMin(value = "0", message = "Visa no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Visa admite máximo 2 decimales") BigDecimal visa,
        @DecimalMin(value = "0", message = "American Express no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "American Express admite máximo 2 decimales") BigDecimal amex,
        @DecimalMin(value = "0", message = "QR no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "QR admite máximo 2 decimales") BigDecimal qr) {
}
