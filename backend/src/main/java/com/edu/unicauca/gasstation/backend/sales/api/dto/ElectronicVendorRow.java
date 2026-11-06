package com.edu.unicauca.gasstation.backend.sales.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ElectronicVendorRow(
        UUID workerId,
        String workerName,
        String shiftCode,
        BigDecimal mastercard,
        BigDecimal visa,
        BigDecimal amex,
        BigDecimal mastercardDawn,
        BigDecimal visaDawn,
        BigDecimal amexDawn,
        BigDecimal planillaCredibanco,
        BigDecimal planillaQr,
        BigDecimal planillaRedemptions) {
}
