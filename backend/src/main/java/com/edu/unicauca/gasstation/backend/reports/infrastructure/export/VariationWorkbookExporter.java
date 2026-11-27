package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles;
import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles.Styles;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Builds the VARIACIÓN Excel workbook with Apache POI.
 * Rows out of tolerance are painted red and incomplete measurements amber.
 *
 * @author javiersolanop777
 */
@Component
public class VariationWorkbookExporter {

    private static final String[] HEADERS = {
            "Combustible", "Inicial", "Entradas", "Vendido", "Teórico", "Medido",
            "Variación", "Tolerancia", "Fuera de tolerancia", "Estado"};

    private static final String STATUS_OK = "OK";

    public byte[] export(VariationReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = Styles.of(workbook);
            Sheet sheet = workbook.createSheet("VARIACIÓN");
            int rowIndex = ExcelWorkbookStyles.writeHeading(sheet, "VARIACIÓN", report.date(), HEADERS, styles);
            ExcelWorkbookStyles.text(sheet.createRow(2), 0, "Origen: " + report.source(), styles.subtitle());

            for (ReportDay.FuelVariation variation : report.variations()) {
                Row row = sheet.createRow(rowIndex++);
                ExcelWorkbookStyles.text(row, 0, variation.fuelType(), styles.text());
                ExcelWorkbookStyles.number(row, 1, variation.initialGallons(), styles.number());
                ExcelWorkbookStyles.number(row, 2, variation.entriesGallons(), styles.number());
                ExcelWorkbookStyles.number(row, 3, variation.soldGallons(), styles.number());
                ExcelWorkbookStyles.number(row, 4, variation.theoreticalGallons(), styles.number());
                ExcelWorkbookStyles.number(row, 5, variation.measuredGallons(), styles.number());
                ExcelWorkbookStyles.number(row, 6, variation.varianceGallons(),
                        variation.outOfTolerance() ? styles.errorNumber() : styles.number());
                ExcelWorkbookStyles.number(row, 7, variation.toleranceGallons(), styles.number());
                ExcelWorkbookStyles.text(row, 8, variation.outOfTolerance() ? "SÍ" : "NO",
                        variation.outOfTolerance() ? styles.error() : styles.ok());
                ExcelWorkbookStyles.text(row, 9, variation.status(), statusStyle(variation.status(), styles));
            }

            rowIndex++;
            Row alerts = sheet.createRow(rowIndex);
            ExcelWorkbookStyles.text(alerts, 0, "Alertas de inventario", styles.totalText());
            ExcelWorkbookStyles.whole(alerts, 1, report.alertCount(), styles.totalInteger());

            ExcelWorkbookStyles.finish(sheet, HEADERS.length);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static CellStyle statusStyle(String status, Styles styles) {
        return STATUS_OK.equalsIgnoreCase(status) ? styles.ok() : styles.warning();
    }
}
