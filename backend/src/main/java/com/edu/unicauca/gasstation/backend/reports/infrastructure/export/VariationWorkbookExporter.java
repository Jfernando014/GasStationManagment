package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.ReportWorkbookSupport.Styles;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Builds the VARIACIÓN Excel workbook with Apache POI.
 *
 * @author javiersolanop777
 */
@Component
public class VariationWorkbookExporter {

    private static final String[] HEADERS = {
            "Combustible", "Inicial", "Entradas", "Vendido", "Teórico", "Medido",
            "Variación", "Tolerancia", "Fuera de tolerancia", "Estado"};

    public byte[] export(VariationReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = Styles.of(workbook);
            Sheet sheet = workbook.createSheet("VARIACIÓN");
            int rowIndex = ReportWorkbookSupport.writeHeading(sheet, "VARIACIÓN", report.date(), HEADERS, styles);
            ReportWorkbookSupport.text(sheet.createRow(2), 0, "Origen: " + report.source(), styles.text());

            for (ReportDay.FuelVariation variation : report.variations()) {
                Row row = sheet.createRow(rowIndex++);
                ReportWorkbookSupport.text(row, 0, variation.fuelType(), styles.text());
                ReportWorkbookSupport.number(row, 1, variation.initialGallons(), styles.number());
                ReportWorkbookSupport.number(row, 2, variation.entriesGallons(), styles.number());
                ReportWorkbookSupport.number(row, 3, variation.soldGallons(), styles.number());
                ReportWorkbookSupport.number(row, 4, variation.theoreticalGallons(), styles.number());
                ReportWorkbookSupport.number(row, 5, variation.measuredGallons(), styles.number());
                ReportWorkbookSupport.number(row, 6, variation.varianceGallons(), styles.number());
                ReportWorkbookSupport.number(row, 7, variation.toleranceGallons(), styles.number());
                ReportWorkbookSupport.text(row, 8, variation.outOfTolerance() ? "SÍ" : "NO", styles.text());
                ReportWorkbookSupport.text(row, 9, variation.status(), styles.text());
            }

            rowIndex++;
            Row alerts = sheet.createRow(rowIndex);
            ReportWorkbookSupport.text(alerts, 0, "Alertas de inventario", styles.totalText());
            ReportWorkbookSupport.whole(alerts, 1, report.alertCount(), styles.totalInteger());

            ReportWorkbookSupport.finish(sheet, HEADERS.length);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
