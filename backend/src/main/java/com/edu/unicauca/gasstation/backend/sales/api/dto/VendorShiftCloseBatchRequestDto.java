package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorShiftCloseBatchRequestDto(
        LocalDate journalDate,
        @NotEmpty List<@Valid VendorShiftCloseItemRequestDto> closes
) {
    public record VendorShiftCloseItemRequestDto(
            UUID id,
            @NotBlank String vendorDocument,
            @NotBlank String vendorName,
            @NotBlank String shiftName,
            @PositiveOrZero Integer pumpNumber,
            @PositiveOrZero BigDecimal cash,
            @PositiveOrZero BigDecimal ga,
            @PositiveOrZero BigDecimal credibanco,
            @PositiveOrZero BigDecimal qr,
            @PositiveOrZero BigDecimal redemptions,
            @PositiveOrZero BigDecimal totalDelivered,
            @PositiveOrZero BigDecimal gallonsMotor,
            @PositiveOrZero BigDecimal gallonsDiesel,
            @PositiveOrZero BigDecimal gallonsExtra,
            @PositiveOrZero BigDecimal gallonsMaxpro,
            @PositiveOrZero BigDecimal basketDominusAmount,
            @PositiveOrZero BigDecimal dominusTirilla,
            List<@Valid ProductItemDto> products
    ) {
        public VendorShiftClose toDomain(LocalDate journalDate) {
            List<VendorCloseProduct> domainProducts = products != null
                    ? products.stream().map(p -> new VendorCloseProduct(
                    p.id(),
                    p.sku(),
                    p.productName(),
                    p.category(),
                    p.unitsSold(),
                    p.unitPrice(),
                    p.totalAmount()
            )).toList()
                    : List.of();

            return new VendorShiftClose(
                    id,
                    journalDate,
                    vendorDocument,
                    vendorName,
                    shiftName,
                    pumpNumber,
                    cash,
                    ga,
                    credibanco,
                    qr,
                    redemptions,
                    totalDelivered,
                    gallonsMotor,
                    gallonsDiesel,
                    gallonsExtra,
                    gallonsMaxpro,
                    basketDominusAmount,
                    dominusTirilla,
                    null,
                    null,
                    null,
                    domainProducts
            );
        }
    }

    public record ProductItemDto(
            UUID id,
            @NotBlank String sku,
            @NotBlank String productName,
            String category,
            @PositiveOrZero int unitsSold,
            @PositiveOrZero BigDecimal unitPrice,
            @PositiveOrZero BigDecimal totalAmount
    ) {}
}
