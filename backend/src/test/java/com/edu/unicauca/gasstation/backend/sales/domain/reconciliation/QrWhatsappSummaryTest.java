package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import static com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QrWhatsappSummaryTest {

    private static VendorQrCheck check(String redeban, String planilla) {
        java.math.BigDecimal r = money(redeban);
        java.math.BigDecimal p = money(planilla);
        return new VendorQrCheck(CARLOS, "Carlos Andrés Paredes", "DIA6", r, p, r.subtract(p),
                r.compareTo(p) == 0);
    }

    @Test
    void matchingVendorSummaryHasDateVendorShiftTotalAndState() {
        String text = QrWhatsappSummary.text(DAY, check("377902", "377902"), true);

        assertEquals("*Cierre QR 05/08/2026*\nVendedor: Carlos Andrés Paredes\nTurno: DIA6\n"
                + "Total QR: $377.902\nEstado: Cuadra", text);
    }

    @Test
    void differenceIsShownWithItsSign() {
        String text = QrWhatsappSummary.text(DAY, check("376902", "377902"), true);

        assertTrue(text.contains("Estado: No cuadra. Diferencia: -$1.000"));
        assertTrue(text.contains("(Redeban $376.902, planilla $377.902)"));
    }

    @Test
    void whileRowsAreUnassignedTheStateIsPendingInsteadOfADifference() {
        String text = QrWhatsappSummary.text(DAY, check("0", "377902"), false);

        assertTrue(text.contains("Estado: Pendiente"));
    }

    @Test
    void copFormatsThousandsWithDotsAndDecimalsWithComma() {
        assertEquals("$1.234.567", QrWhatsappSummary.cop(money("1234567")));
        assertEquals("$1.234,5", QrWhatsappSummary.cop(money("1234.50")));
        assertEquals("-$19.760", QrWhatsappSummary.cop(money("-19760")));
        assertEquals("$0", QrWhatsappSummary.cop(money("0")));
    }
}
