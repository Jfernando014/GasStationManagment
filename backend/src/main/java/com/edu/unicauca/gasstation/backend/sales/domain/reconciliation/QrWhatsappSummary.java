package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Text the administrator copies and sends to a vendor: date, vendor, shift, QR total and reconciliation state. */
public final class QrWhatsappSummary {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private QrWhatsappSummary() {
    }

    /**
     * @param reliable false while Redeban rows remain unassigned: a vendor that does not match may only be missing
     *                 rows, so the text says the check is pending instead of declaring a difference.
     */
    public static String text(LocalDate date, VendorQrCheck vendor, boolean reliable) {
        StringBuilder text = new StringBuilder();
        text.append("*Cierre QR ").append(DATE.format(date)).append("*\n");
        text.append("Vendedor: ").append(vendor.workerName() == null ? "-" : vendor.workerName()).append('\n');
        text.append("Turno: ").append(vendor.shiftCode() == null ? "-" : vendor.shiftCode()).append('\n');
        text.append("Total QR: ").append(cop(vendor.redeban())).append('\n');
        if (vendor.matches()) {
            text.append("Estado: Cuadra");
        } else if (!reliable) {
            text.append("Estado: Pendiente, hay transacciones sin asignar a un vendedor");
        } else {
            text.append("Estado: No cuadra. Diferencia: ").append(cop(vendor.difference()))
                    .append(" (Redeban ").append(cop(vendor.redeban()))
                    .append(", planilla ").append(cop(vendor.planilla())).append(')');
        }
        return text.toString();
    }

    /** Colombian format: $1.234.567 (and a comma for decimals, only when there are any). */
    static String cop(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        String formatted = new DecimalFormat("#,##0.##", symbols).format(value.abs());
        return (value.signum() < 0 ? "-" : "") + "$" + formatted;
    }
}
