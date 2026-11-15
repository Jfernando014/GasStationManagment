package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import com.edu.unicauca.gasstation.backend.reports.domain.models.DominusComplementReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.ReportWorkbookSupport.Styles;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Builds the COMPLEMENTO DOMINUS Excel workbook with Apache POI.
 *
 * @author javiersolanop777
 */
@Component
public class DominusComplementWorkbookExporter {

    private static final String[] HEADERS = {
            "Vendedor", "Documento", "Turno", "Galones", "Canastilla",
            "Total entregado", "Tirilla Dominus", "Diferencia", "Resultado"};

    public byte[] export(DominusComplementReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = Styles.of(workbook);
            Sheet sheet = workbook.createSheet("COMPLEMENTO DOMINUS");
            int rowIndex = ReportWorkbookSupport.writeHeading(sheet, "COMPLEMENTO DOMINUS", report.date(),
                    HEADERS, styles);
            ReportWorkbookSupport.text(sheet.createRow(2), 0, "Origen: " + report.source(), styles.text());

            rowIndex = writeCashSummary(sheet, rowIndex, report, styles);
            rowIndex++;

            for (ReportDay.VendorRow vendor : report.vendors()) {
                Row row = sheet.createRow(rowIndex++);
                ReportWorkbookSupport.text(row, 0, vendor.vendorName(), styles.text());
                ReportWorkbookSupport.text(row, 1, vendor.vendorDocument(), styles.text());
                ReportWorkbookSupport.text(row, 2, vendor.shiftName(), styles.text());
                ReportWorkbookSupport.number(row, 3, vendor.gallons(), styles.number());
                ReportWorkbookSupport.number(row, 4, vendor.basketAmount(), styles.number());
                ReportWorkbookSupport.number(row, 5, vendor.totalDelivered(), styles.number());
                ReportWorkbookSupport.number(row, 6, vendor.dominusTirilla(), styles.number());
                ReportWorkbookSupport.number(row, 7, vendor.discrepancy(), styles.number());
                ReportWorkbookSupport.text(row, 8, vendor.resultStatus(), styles.text());
            }

            Row totals = sheet.createRow(rowIndex);
            ReportWorkbookSupport.text(totals, 0, "TOTAL DEL DÍA", styles.totalText());
            ReportWorkbookSupport.number(totals, 4, report.totalBasket(), styles.totalNumber());
            ReportWorkbookSupport.number(totals, 5, report.totalDelivered(), styles.totalNumber());
            ReportWorkbookSupport.number(totals, 6, report.totalDominusTirilla(), styles.totalNumber());
            ReportWorkbookSupport.number(totals, 7, report.totalDiscrepancy(), styles.totalNumber());

            ReportWorkbookSupport.finish(sheet, HEADERS.length);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static int writeCashSummary(Sheet sheet, int rowIndex, DominusComplementReport report, Styles styles) {
        ReportDay.CashSummary cash = report.cash();
        if (cash == null) {
            Row row = sheet.createRow(rowIndex++);
            ReportWorkbookSupport.text(row, 0, "Saldo en caja", styles.totalText());
            ReportWorkbookSupport.text(row, 1, "No disponible", styles.text());
            return rowIndex;
        }
        rowIndex = cashRow(sheet, rowIndex, "Planilla No.", cash.planilla() == null ? null : cash.planilla().toString(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Estado de la jornada", cash.journalStatus(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo en caja", cash.cashBalance(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Efectivo del día", cash.dailyCash(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo planilla", cash.planillaCashBalance(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo Dominus", cash.dominusAccumulatedBalance(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Diferencia", cash.difference(), styles);
        rowIndex = cashRow(sheet, rowIndex, "Saldo anterior ahorro", cash.previousCash(), styles);
        return cashRow(sheet, rowIndex, "Ahorro acumulado", cash.accumulatedSavings(), styles);
    }

    private static int cashRow(Sheet sheet, int rowIndex, String label, Object value, Styles styles) {
        Row row = sheet.createRow(rowIndex++);
        ReportWorkbookSupport.text(row, 0, label, styles.totalText());
        if (value instanceof java.math.BigDecimal decimal) {
            ReportWorkbookSupport.number(row, 1, decimal, styles.totalNumber());
        } else {
            ReportWorkbookSupport.text(row, 1, value == null ? "No disponible" : value.toString(), styles.text());
        }
        return rowIndex;
    }
}
