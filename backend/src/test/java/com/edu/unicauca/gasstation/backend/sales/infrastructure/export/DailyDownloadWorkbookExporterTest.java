package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import static com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.day;
import static com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.vendor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Level 1: checks the sheets, columns and totals of the daily Excel files.
 *
 * @author javiersolanop777
 */
class DailyDownloadWorkbookExporterTest {

    private final DailyDownloadWorkbookExporter atrExporter = new DailyDownloadWorkbookExporter();

    @Test
    void salesSheetHasVendorRowAdditiveUnitsAndDayTotal() throws Exception {
        byte[] varBytes = atrExporter.exportSalesSheet(day(), List.of(vendor()));
        try (Workbook objWorkbook = new XSSFWorkbook(new ByteArrayInputStream(varBytes))) {
            Sheet objSheet = objWorkbook.getSheet("PLANILLA VENTAS");
            assertNotNull(objSheet);
            assertEquals("Unidades aditivos", objSheet.getRow(3).getCell(9).getStringCellValue());
            assertEquals("Vendedor Test", objSheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals(35.0, objSheet.getRow(4).getCell(8).getNumericCellValue());
            assertEquals(2.0, objSheet.getRow(4).getCell(9).getNumericCellValue());
            assertEquals(100000.0, objSheet.getRow(4).getCell(10).getNumericCellValue());
            assertEquals("TOTAL DEL DÍA", objSheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals(2.0, objSheet.getRow(5).getCell(9).getNumericCellValue());
            assertEquals(100000.0, objSheet.getRow(5).getCell(10).getNumericCellValue());
        }
    }

    @Test
    void lubricantsFileGroupsProductsLikeTheClientSummary() throws Exception {
        byte[] varBytes = atrExporter.exportLubricants(day());
        try (Workbook objWorkbook = new XSSFWorkbook(new ByteArrayInputStream(varBytes))) {
            assertEquals(1, objWorkbook.getNumberOfSheets());
            Sheet objSheet = objWorkbook.getSheet("LUBRICANTES Y VARIOS");
            assertEquals("ACEITES", objSheet.getRow(4).getCell(2).getStringCellValue());
            assertEquals("MERCANCIA VARIA", objSheet.getRow(5).getCell(2).getStringCellValue());
            assertEquals("Bolsas Test", objSheet.getRow(5).getCell(1).getStringCellValue());
            assertEquals("ADITIVOS Y OTROS", objSheet.getRow(6).getCell(2).getStringCellValue());
            assertEquals("Liquido Frenos Test", objSheet.getRow(7).getCell(1).getStringCellValue());
            assertEquals("ADITIVOS Y OTROS", objSheet.getRow(7).getCell(2).getStringCellValue());
            assertEquals(11.0, objSheet.getRow(8).getCell(4).getNumericCellValue());
            assertEquals(100000.0, objSheet.getRow(8).getCell(5).getNumericCellValue());
            assertNull(objSheet.getRow(9));
            assertEquals("VENTA ACEITES", objSheet.getRow(10).getCell(1).getStringCellValue());
            assertEquals(60000.0, objSheet.getRow(10).getCell(5).getNumericCellValue());
            assertEquals(5000.0, objSheet.getRow(11).getCell(5).getNumericCellValue());
            assertEquals(35000.0, objSheet.getRow(12).getCell(5).getNumericCellValue());
            assertEquals("VENTA TOTAL CANASTA", objSheet.getRow(13).getCell(1).getStringCellValue());
            assertEquals(100000.0, objSheet.getRow(13).getCell(5).getNumericCellValue());
        }
    }
}
