package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.AMBER_FILL;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.GREEN_FILL;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.RED_FILL;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertFill;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertHeader;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertNumberFormat;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertTotal;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.bd;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.variationReport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
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

    @Test
    void paintsHeadersTotalsAndTheStatusOfEveryVariation() throws Exception {
        VariationReport report = new VariationReport(DATE, ReportDay.Source.LIVE,
                List.of(variation("MOTOR", "12.50", false, "OK"),
                        variation("EXTRA", "80.00", true, "OK"),
                        variation("DIESEL", "0", false, "INCOMPLETE_MEASUREMENT")), 1);

        byte[] bytes = new VariationWorkbookExporter().export(report);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("VARIACIÓN");
            assertHeader(sheet.getRow(3).getCell(0));
            assertNumberFormat(sheet.getRow(4).getCell(1), ExcelWorkbookStyles.GALLONS_FORMAT);
            assertFill(sheet.getRow(4).getCell(8), GREEN_FILL);
            assertFill(sheet.getRow(5).getCell(6), RED_FILL);
            assertFill(sheet.getRow(6).getCell(9), AMBER_FILL);
            assertTotal(sheet.getRow(8).getCell(0));
            assertNumberFormat(sheet.getRow(8).getCell(1), ExcelWorkbookStyles.WHOLE_FORMAT);
            assertTrue(((XSSFSheet) sheet).getPaneInformation().isFreezePane(), "The header must stay frozen");
        }
    }

    private static ReportDay.FuelVariation variation(String fuel, String variance, boolean outOfTolerance,
            String status) {
        return new ReportDay.FuelVariation(fuel, bd("4500"), bd("0"), bd("480"), bd("4020"), bd("4032.50"),
                bd(variance), bd("65"), outOfTolerance, status);
    }
}
