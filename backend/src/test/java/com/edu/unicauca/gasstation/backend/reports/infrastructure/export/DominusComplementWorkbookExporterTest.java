package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.dominusReport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests the COMPLEMENTO DOMINUS Excel export.
 *
 * @author javiersolanop777
 */
class DominusComplementWorkbookExporterTest {

    @Test
    void writesTheCashSummaryVendorsAndTotals() throws Exception {
        byte[] bytes = new DominusComplementWorkbookExporter().export(dominusReport());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("COMPLEMENTO DOMINUS");
            assertNotNull(sheet);
            assertEquals("Planilla No.", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("289", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("Carlos M.", sheet.getRow(14).getCell(0).getStringCellValue());
            assertEquals(310.0, sheet.getRow(14).getCell(3).getNumericCellValue());
            assertEquals("TOTAL DEL DÍA", sheet.getRow(15).getCell(0).getStringCellValue());
            assertEquals(569469.0, sheet.getRow(15).getCell(5).getNumericCellValue());
        }
    }
}
