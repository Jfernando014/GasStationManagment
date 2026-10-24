package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorShiftCloseBatchRequestDto(
        LocalDate journalDate,
        @NotEmpty List<VendorShiftCloseItemRequestDto> closes
) {
    public record VendorShiftCloseItemRequestDto(
            UUID id,
            @NotBlank String vendorDocument,
            @NotBlank String vendorName,
            @NotBlank String shiftName,
            Integer pumpNumber,
            BigDecimal cash,
            BigDecimal ga,
            BigDecimal credibanco,
            BigDecimal qr,
            BigDecimal redemptions,
            BigDecimal totalDelivered,
            BigDecimal gallonsMotor,
            BigDecimal gallonsDiesel,
            BigDecimal gallonsExtra,
            BigDecimal gallonsMaxpro,
            BigDecimal basketDominusAmount,
            BigDecimal dominusTirilla,
            List<ProductItemDto> products
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
            String sku,
            String productName,
            String category,
            int unitsSold,
            BigDecimal unitPrice,
            BigDecimal totalAmount
    ) {}
}
