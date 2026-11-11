package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;

import java.time.LocalDate;

public final class InspectionTestData {

    private InspectionTestData() {
    }

    public static Inspection inspection(Long id, String code, InspectionCategory category,
                                        LocalDate dateExecuted, LocalDate dateDue) {
        Inspection inspection = new Inspection();
        inspection.setId(id);
        inspection.setCode(code);
        inspection.setCategory(category);
        inspection.setTarget("Dispensador 1 y 2 (Mangueras 1 a 6)");
        inspection.setDateExecuted(dateExecuted);
        inspection.setDateDue(dateDue);
        inspection.setCertifier("Metrología del Cauca SAS (ONAC 19-LAB-042)");
        inspection.setToleranceMeasured("+3,8 mL (Límite ±15 mL)");
        inspection.setCertDoc("CERT-ONAC-0812.pdf");
        inspection.setStatus(InspectionStatus.VIGENTE);
        inspection.setCreatedAt(dateExecuted.atTime(8, 0));
        inspection.setUpdatedAt(dateExecuted.atTime(8, 0));
        return inspection;
    }
}
