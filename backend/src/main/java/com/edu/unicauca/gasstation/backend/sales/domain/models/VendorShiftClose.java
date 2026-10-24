package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record VendorShiftClose(
        UUID id,
        LocalDate journalDate,
        String vendorDocument,
        String vendorName,
        String shiftName,
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
        BigDecimal realDiscrepancy,
        BigDecimal appliedTolerance,
        DiscrepancyStatus resultStatus,
        List<VendorCloseProduct> products
) {
    public VendorShiftClose {
        Objects.requireNonNull(journalDate, "journalDate is required");
        Objects.requireNonNull(vendorDocument, "vendorDocument is required");
        Objects.requireNonNull(vendorName, "vendorName is required");
        Objects.requireNonNull(shiftName, "shiftName is required");

        cash = (cash != null ? cash : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        ga = (ga != null ? ga : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        credibanco = (credibanco != null ? credibanco : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        qr = (qr != null ? qr : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        redemptions = (redemptions != null ? redemptions : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal calculatedDelivered = cash.add(ga).add(credibanco).add(qr).add(redemptions)
                .setScale(2, RoundingMode.HALF_UP);
        totalDelivered = totalDelivered != null
                ? totalDelivered.setScale(2, RoundingMode.HALF_UP)
                : calculatedDelivered;

        gallonsMotor = (gallonsMotor != null ? gallonsMotor : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
        gallonsDiesel = (gallonsDiesel != null ? gallonsDiesel : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
        gallonsExtra = (gallonsExtra != null ? gallonsExtra : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
        gallonsMaxpro = (gallonsMaxpro != null ? gallonsMaxpro : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);

        basketDominusAmount = (basketDominusAmount != null ? basketDominusAmount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        dominusTirilla = (dominusTirilla != null ? dominusTirilla : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal calculatedDiscrepancy = totalDelivered.subtract(dominusTirilla).setScale(2, RoundingMode.HALF_UP);
        realDiscrepancy = realDiscrepancy != null
                ? realDiscrepancy.setScale(2, RoundingMode.HALF_UP)
                : calculatedDiscrepancy;

        appliedTolerance = (appliedTolerance != null ? appliedTolerance : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        resultStatus = resultStatus != null ? resultStatus : DiscrepancyStatus.CUADRA;
        products = products != null ? List.copyOf(products) : List.of();
    }
}
