package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.variationReport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests the VARIACIÓN Excel export.
 *
 * @author javiersolanop777
 */
class VariationWorkbookExporterTest {

    @Test
    void writesTheVariationSheetWithHeadersDataAndAlerts() throws Exception {
        byte[] bytes = new VariationWorkbookExporter().export(variationReport());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("VARIACIÓN");
            assertNotNull(sheet);
            assertEquals("Combustible", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Fuera de tolerancia", sheet.getRow(3).getCell(8).getStringCellValue());
            assertEquals("MOTOR", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals(4500.0, sheet.getRow(4).getCell(1).getNumericCellValue());
            assertEquals(12.5, sheet.getRow(4).getCell(6).getNumericCellValue());
            assertEquals("NO", sheet.getRow(4).getCell(8).getStringCellValue());
            assertEquals("Alertas de inventario", sheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals(2.0, sheet.getRow(6).getCell(1).getNumericCellValue());
        }
    }
}
