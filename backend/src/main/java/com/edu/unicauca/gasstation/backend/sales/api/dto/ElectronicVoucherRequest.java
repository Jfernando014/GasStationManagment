package com.edu.unicauca.gasstation.backend.sales.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record ElectronicVoucherRequest(
        @DecimalMin(value = "0", message = "Mastercard no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Mastercard admite máximo 2 decimales")
        BigDecimal mastercard,
        @DecimalMin(value = "0", message = "Visa no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Visa admite máximo 2 decimales")
        BigDecimal visa,
        @DecimalMin(value = "0", message = "American Express no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "American Express admite máximo 2 decimales")
        BigDecimal amex,
        @DecimalMin(value = "0", message = "Mastercard madrugada no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Mastercard madrugada admite máximo 2 decimales")
        BigDecimal mastercardDawn,
        @DecimalMin(value = "0", message = "Visa madrugada no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Visa madrugada admite máximo 2 decimales")
        BigDecimal visaDawn,
        @DecimalMin(value = "0", message = "American Express madrugada no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "American Express madrugada admite máximo 2 decimales")
        BigDecimal amexDawn) {
}
