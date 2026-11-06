package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.CredibancoDayResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PointsResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.QrReconciliationResult;
import java.time.LocalDate;
import java.util.List;

public record ElectronicPaymentsStatus(
        LocalDate date,
        boolean canComplete,
        List<Blocker> blockers,
        List<ElectronicVendorRow> vendors,
        CredibancoDayResult credibanco,
        QrReconciliationResult qr,
        PointsResult points) {

    public ElectronicPaymentsStatus {
        blockers = List.copyOf(blockers);
        vendors = List.copyOf(vendors);
    }

    public record Blocker(String code, String message) {
    }
}
