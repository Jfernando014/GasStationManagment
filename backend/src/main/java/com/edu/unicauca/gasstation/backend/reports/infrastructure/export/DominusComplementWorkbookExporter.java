package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles;
import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles.Styles;
import com.edu.unicauca.gasstation.backend.reports.domain.models.DominusComplementReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.shifts.catalog.CustomShiftCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Builds the COMPLEMENTO DOMINUS Excel workbook with Apache POI.
 * Balanced vendors are painted green, surplus amber and shortfall red.
 *
 * @author javiersolanop777
 */
@Component
public class DominusComplementWorkbookExporter {

    private static final String[] HEADERS = {
            "Vendedor", "Documento", "Turno", "Galones", "Canastilla",
            "Total entregado", "Tirilla Dominus", "Diferencia", "Resultado"};

    private static final String RESULT_BALANCED = "CUADRA";
    private static final String RESULT_SHORTFALL = "FALTA";

    public byte[] export(DominusComplementReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = Styles.of(workbook);
            Sheet sheet = workbook.createSheet("COMPLEMENTO DOMINUS");
            int rowIndex = ExcelWorkbookStyles.writeHeading(sheet, "COMPLEMENTO DOMINUS", report.date(),
                    HEADERS, styles);
            ExcelWorkbookStyles.text(sheet.createRow(2), 0, "Origen: " + report.source(), styles.subtitle());

            rowIndex = writeCashSummary(sheet, rowIndex, report, styles);
            rowIndex++;

            for (ReportDay.VendorRow vendor : report.vendors()) {
                Row row = sheet.createRow(rowIndex++);
                CellStyle differenceStyle = differenceStyle(vendor.resultStatus(), styles);
                ExcelWorkbookStyles.text(row, 0, vendor.vendorName(), styles.text());
                ExcelWorkbookStyles.text(row, 1, vendor.vendorDocument(), styles.text());
                ExcelWorkbookStyles.text(row, 2, CustomShiftCode.displayText(vendor.shiftName()), styles.text());
                ExcelWorkbookStyles.number(row, 3, vendor.gallons(), styles.number());
                ExcelWorkbookStyles.number(row, 4, vendor.basketAmount(), styles.money());
                ExcelWorkbookStyles.number(row, 5, vendor.totalDelivered(), styles.money());
                ExcelWorkbookStyles.number(row, 6, vendor.dominusTirilla(), styles.money());
                ExcelWorkbookStyles.number(row, 7, vendor.discrepancy(), differenceStyle);
                ExcelWorkbookStyles.text(row, 8, vendor.resultStatus(), resultStyle(vendor.resultStatus(), styles));
            }

            Row totals = sheet.createRow(rowIndex);
            ExcelWorkbookStyles.text(totals, 0, "TOTAL DEL DÍA", styles.totalText());
            ExcelWorkbookStyles.number(totals, 4, report.totalBasket(), styles.totalMoney());
            ExcelWorkbookStyles.number(totals, 5, report.totalDelivered(), styles.totalMoney());
            ExcelWorkbookStyles.number(totals, 6, report.totalDominusTirilla(), styles.totalMoney());
            ExcelWorkbookStyles.number(totals, 7, report.totalDiscrepancy(), styles.totalMoney());

            ExcelWorkbookStyles.finish(sheet, HEADERS.length);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static int writeCashSummary(Sheet sheet, int rowIndex, DominusComplementReport report, Styles styles) {
        ReportDay.CashSummary cash = report.cash();
        if (cash == null) {
            Row row = sheet.createRow(rowIndex++);
            ExcelWorkbookStyles.text(row, 0, "Saldo en caja", styles.section());
            ExcelWorkbookStyles.text(row, 1, "No disponible", styles.text());
            return rowIndex;
        }
        rowIndex = cashRow(sheet, rowIndex, "Planilla No.", cash.planilla() == null ? null : cash.planilla().toString(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Estado de la jornada", cash.journalStatus(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo en caja", cash.cashBalance(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Efectivo del día", cash.dailyCash(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo planilla", cash.planillaCashBalance(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo Dominus", cash.dominusAccumulatedBalance(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Diferencia", cash.difference(), styles, true);
        rowIndex = cashRow(sheet, rowIndex, "Saldo anterior ahorro", cash.previousCash(), styles);
        return cashRow(sheet, rowIndex, "Ahorro acumulado", cash.accumulatedSavings(), styles);
    }

    private static int cashRow(Sheet sheet, int rowIndex, String label, Object value, Styles styles) {
        return cashRow(sheet, rowIndex, label, value, styles, false);
    }

    private static int cashRow(Sheet sheet, int rowIndex, String label, Object value, Styles styles, boolean alert) {
        Row row = sheet.createRow(rowIndex++);
        ExcelWorkbookStyles.text(row, 0, label, styles.section());
        if (value instanceof BigDecimal decimal) {
            ExcelWorkbookStyles.number(row, 1, decimal, alert ? cashStyle(decimal, styles) : styles.money());
        } else {
            ExcelWorkbookStyles.text(row, 1, value == null ? "No disponible" : value.toString(), styles.text());
        }
        return rowIndex;
    }

    /** A cash difference of zero is balanced; any other value needs the attention of the reader. */
    private static CellStyle cashStyle(BigDecimal value, Styles styles) {
        return value.signum() == 0 ? styles.okMoney() : styles.warningMoney();
    }

    private static CellStyle resultStyle(String resultStatus, Styles styles) {
        if (resultStatus == null || RESULT_BALANCED.equalsIgnoreCase(resultStatus)) {
            return styles.ok();
        }
        return RESULT_SHORTFALL.equalsIgnoreCase(resultStatus) ? styles.error() : styles.warning();
    }

    private static CellStyle differenceStyle(String resultStatus, Styles styles) {
        if (resultStatus == null || RESULT_BALANCED.equalsIgnoreCase(resultStatus)) {
            return styles.okMoney();
        }
        return RESULT_SHORTFALL.equalsIgnoreCase(resultStatus) ? styles.errorMoney() : styles.warningMoney();
    }
}
