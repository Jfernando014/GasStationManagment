package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.VendorDeclaration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Vouchers by franchise of one vendor and the figures of his planilla. Blank amounts are 0.")
public record CredibancoVendorRequest(
        @Schema(description = "Shift code of the vendor that day (DIA, NOCHE, 12-7...); used to attribute Redeban QR rows",
                example = "DIA")
        @Size(max = 40, message = "El código de turno admite máximo 40 caracteres") String shiftCode,
        @DecimalMin(value = "0", message = "Mastercard no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Mastercard admite máximo 2 decimales") BigDecimal mastercard,
        @DecimalMin(value = "0", message = "Visa no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Visa admite máximo 2 decimales") BigDecimal visa,
        @DecimalMin(value = "0", message = "American Express no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "American Express admite máximo 2 decimales") BigDecimal amex,
        @DecimalMin(value = "0", message = "Mastercard madrugada no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Mastercard madrugada admite máximo 2 decimales") BigDecimal mastercardDawn,
        @DecimalMin(value = "0", message = "Visa madrugada no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Visa madrugada admite máximo 2 decimales") BigDecimal visaDawn,
        @DecimalMin(value = "0", message = "American Express madrugada no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "American Express madrugada admite máximo 2 decimales") BigDecimal amexDawn,
        @DecimalMin(value = "0", message = "El Credibanco de la planilla no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El Credibanco de la planilla admite máximo 2 decimales") BigDecimal planillaCredibanco,
        @DecimalMin(value = "0", message = "El QR de la planilla no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El QR de la planilla admite máximo 2 decimales") BigDecimal planillaQr,
        @DecimalMin(value = "0", message = "Las redenciones de la planilla no pueden ser negativas")
        @Digits(integer = 12, fraction = 2, message = "Las redenciones de la planilla admiten máximo 2 decimales") BigDecimal planillaRedemptions) {

    public VendorDeclaration toDomain(UUID workerId) {
        return new VendorDeclaration(workerId, shiftCode, mastercard, visa, amex, mastercardDawn, visaDawn,
                amexDawn, planillaCredibanco, planillaQr, planillaRedemptions);
    }
}
