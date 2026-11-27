package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.AMBER_FILL;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.GREEN_FILL;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.RED_FILL;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertFill;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertHeader;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertNumberFormat;
import static com.edu.unicauca.gasstation.backend.excel.ExcelStyleAssertions.assertTotal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpenseItemInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalFullWorkbookData;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class JournalWorkbookExporterTest {

    private final JournalWorkbookExporter exporter = new JournalWorkbookExporter();

    @Test
    void exportsAllOperationalSheetsWithConsolidatedTotals() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 5);
        JournalSnapshot snapshot = new JournalSnapshot(
                10L,
                date,
                289L,
                JournalStatus.CLOSED,
                LocalDateTime.of(2026, 8, 5, 6, 0),
                List.of(
                        new JournalSnapshot.JournalStepSnapshot(1, "Personal", StepStatus.COMPLETED, LocalDateTime.of(2026, 8, 5, 7, 0)),
                        new JournalSnapshot.JournalStepSnapshot(6, "Cierre", StepStatus.COMPLETED, LocalDateTime.of(2026, 8, 5, 19, 0))),
                "Surtidor 2 presentó reinicio breve en la tarde.");

        ProductSaleSummary product = new ProductSaleSummary(
                "ADD-01", "Aditivo Ruta 75", "ADITIVO", 3, new BigDecimal("12000"), new BigDecimal("36000"));

        VendorCloseSummary carlos = new VendorCloseSummary(
                date, "1001", "Carlos P.", "DIA6", 1,
                new BigDecimal("4000000"), new BigDecimal("50000"), new BigDecimal("2500000"),
                new BigDecimal("400000"), new BigDecimal("20000"), new BigDecimal("6970000"),
                new BigDecimal("300.50"), new BigDecimal("150.25"), new BigDecimal("50.00"), new BigDecimal("30.67"),
                new BigDecimal("36000"), new BigDecimal("6970000"), BigDecimal.ZERO, "CUADRA", List.of(product));

        DaySummary daySummary = new DaySummary(
                date,
                new BigDecimal("4000000"), new BigDecimal("50000"), new BigDecimal("2500000"),
                new BigDecimal("400000"), new BigDecimal("20000"), new BigDecimal("6970000"),
                new BigDecimal("6970000"), BigDecimal.ZERO,
                new BigDecimal("300.50"), new BigDecimal("150.25"), new BigDecimal("50.00"), new BigDecimal("30.67"),
                new BigDecimal("531.42"), new BigDecimal("36000"), 3, List.of(product));

        CashBalanceTableInfo cashBalance = new CashBalanceTableInfo(
                date, 289L, "CLOSED",
                new SavingsFundInfo(new BigDecimal("2750000"), new BigDecimal("250000"), BigDecimal.ZERO, new BigDecimal("3000000")),
                new ExpensesInfo(List.of(new ExpenseItemInfo(date, "Papelería oficina", new BigDecimal("25000"))), new BigDecimal("25000")),
                new BigDecimal("1200000"), new BigDecimal("4000000"), new BigDecimal("5200000"),
                new BigDecimal("5199469"), new BigDecimal("-531"));

        ClosingInventorySnapshot inventory = new ClosingInventorySnapshot(
                true,
                List.of(new FuelVariationSummary(
                        "CORRIENTE",
                        new BigDecimal("5000.00"), new BigDecimal("1000.00"), new BigDecimal("300.50"),
                        new BigDecimal("5699.50"), new BigDecimal("5759.40"), new BigDecimal("59.90"),
                        new BigDecimal("100.00"), false, "NORMAL"),
                        new FuelVariationSummary(
                                "EXTRA",
                                new BigDecimal("1000.00"), new BigDecimal("200.00"), new BigDecimal("50.00"),
                                new BigDecimal("1150.00"), new BigDecimal("1200.00"), new BigDecimal("50.00"),
                                new BigDecimal("10.00"), true, "FUERA DE TOLERANCIA")),
                List.of(new InventoryAlertSummary(
                        1L, "AUTONOMY", "EXTRA", "WARNING", "OPEN", new BigDecimal("2.5"), date)));

        JournalFullWorkbookData fullData = new JournalFullWorkbookData(
                snapshot,
                List.of(
                        new AssignedVendor("1001", "Carlos P.", "TITULAR", "DIA6", 1, "V-01"),
                        new AssignedVendor("1002", "Mónica D.", "APOYO", "AP_10-17", 2, "V-02")),
                daySummary,
                List.of(carlos),
                cashBalance,
                inventory);

        byte[] bytes = exporter.export(fullData);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(7, wb.getNumberOfSheets());
            assertEquals("Jornada", wb.getSheetAt(0).getSheetName());
            assertEquals("Pasos", wb.getSheetAt(1).getSheetName());
            assertEquals("Personal", wb.getSheetAt(2).getSheetName());
            assertEquals("Cierre Vendedores", wb.getSheetAt(3).getSheetName());
            assertEquals("Lubricantes y Otros", wb.getSheetAt(4).getSheetName());
            assertEquals("Saldo en Caja", wb.getSheetAt(5).getSheetName());
            assertEquals("Inventario y Variación", wb.getSheetAt(6).getSheetName());

            XSSFSheet staffSheet = wb.getSheet("Personal");
            assertEquals("Carlos P.", staffSheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("DIA6", staffSheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("10:00–17:00", staffSheet.getRow(2).getCell(4).getStringCellValue());

            XSSFSheet vendorSheet = wb.getSheet("Cierre Vendedores");
            assertEquals("Carlos P.", vendorSheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(531.42, vendorSheet.getRow(1).getCell(8).getNumericCellValue(), 0.001);
            assertEquals("TOTAL", vendorSheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(6970000.0, vendorSheet.getRow(2).getCell(15).getNumericCellValue(), 0.01);

            XSSFSheet productsSheet = wb.getSheet("Lubricantes y Otros");
            assertEquals("Aditivo Ruta 75", productsSheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(3.0, productsSheet.getRow(2).getCell(3).getNumericCellValue(), 0.01);

            XSSFSheet cashSheet = wb.getSheet("Saldo en Caja");
            assertTrue(cashSheet.getLastRowNum() >= 15);
            assertNumberFormat(cashSheet.getRow(12).getCell(1), ExcelWorkbookStyles.MONEY_FORMAT);
            assertFill(cashSheet.getRow(12).getCell(1), AMBER_FILL);

            XSSFSheet invSheet = wb.getSheet("Inventario y Variación");
            assertEquals("CORRIENTE", invSheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(59.90, invSheet.getRow(1).getCell(6).getNumericCellValue(), 0.01);
            assertFill(invSheet.getRow(2).getCell(6), RED_FILL);
            assertFill(invSheet.getRow(2).getCell(8), RED_FILL);

            XSSFSheet summarySheet = wb.getSheet("Jornada");
            assertEquals(
                    "Surtidor 2 presentó reinicio breve en la tarde.",
                    summarySheet.getRow(summarySheet.getLastRowNum()).getCell(1).getStringCellValue());

            assertHeader(summarySheet.getRow(0).getCell(0));
            assertNumberFormat(summarySheet.getRow(9).getCell(1), ExcelWorkbookStyles.MONEY_FORMAT);

            XSSFSheet stepsSheet = wb.getSheet("Pasos");
            assertHeader(stepsSheet.getRow(0).getCell(2));
            assertFill(stepsSheet.getRow(1).getCell(2), GREEN_FILL);
            assertTrue(stepsSheet.getPaneInformation().isFreezePane(), "Headers must stay frozen");

            assertNumberFormat(vendorSheet.getRow(1).getCell(15), ExcelWorkbookStyles.MONEY_FORMAT);
            assertEquals(HorizontalAlignment.RIGHT, vendorSheet.getRow(1).getCell(15).getCellStyle().getAlignment(),
                    "Numeric cells must be right aligned");
            assertEquals(30f, vendorSheet.getRow(0).getHeightInPoints(), 0.01f,
                    "The header row needs room for two lines");
            assertFill(vendorSheet.getRow(1).getCell(17), GREEN_FILL);
            assertFill(vendorSheet.getRow(1).getCell(18), GREEN_FILL);
            assertTotal(vendorSheet.getRow(2).getCell(0));
            assertNumberFormat(vendorSheet.getRow(2).getCell(15), ExcelWorkbookStyles.MONEY_FORMAT);
        }
    }

    @Test
    void exportsGracefullyWhenOptionalOperationalDataIsAbsent() throws Exception {
        JournalSnapshot snapshot = new JournalSnapshot(
                7L, LocalDate.of(2026, 10, 12), 42L, JournalStatus.OPEN,
                LocalDateTime.of(2026, 10, 12, 8, 0), List.of());

        byte[] bytes = exporter.export(snapshot);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(7, wb.getNumberOfSheets());
            assertEquals("2026-10-12", wb.getSheet("Jornada").getRow(2).getCell(1).getStringCellValue());
            assertEquals("Sin personal registrado para esta jornada", wb.getSheet("Personal").getRow(1).getCell(0).getStringCellValue());
            assertEquals("Sin registro de saldo en caja para esta jornada", wb.getSheet("Saldo en Caja").getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportsStepObservationsNativeExcelCommentsAndNotesSheet() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 24);
        JournalSnapshot snapshot = new JournalSnapshot(
                22L,
                date,
                305L,
                JournalStatus.CLOSED,
                LocalDateTime.of(2026, 11, 24, 6, 0),
                List.of(
                        new JournalSnapshot.JournalStepSnapshot(
                                1, "Personal", StepStatus.COMPLETED, LocalDateTime.of(2026, 11, 24, 7, 0),
                                "Turno de apoyo inició a las 10:00."),
                        new JournalSnapshot.JournalStepSnapshot(
                                2, "Cierre por vendedor", StepStatus.COMPLETED, LocalDateTime.of(2026, 11, 24, 15, 0),
                                "Se verificó bono en efectivo con recibo.")),
                "Jornada cerrada sin novedades críticas.",
                List.of(
                        new JournalCellNote(1L, 22L, 2, "1001", "parciales", "Sobre #2 con novedad de billete.", LocalDateTime.of(2026, 11, 24, 13, 15)),
                        new JournalCellNote(2L, 22L, 2, "1001", "pico", "Cambio entregado en monedas.", LocalDateTime.of(2026, 11, 24, 13, 20)),
                        new JournalCellNote(3L, 22L, 2, "1001", "credibanco", "Pasado en datafono alterno.", LocalDateTime.of(2026, 11, 24, 14, 0))));

        VendorCloseSummary carlos = new VendorCloseSummary(
                date, "1001", "Carlos P.", "DIA6", 1,
                new BigDecimal("4000000"), new BigDecimal("50000"), new BigDecimal("2500000"),
                new BigDecimal("400000"), new BigDecimal("20000"), new BigDecimal("6970000"),
                new BigDecimal("300.50"), new BigDecimal("150.25"), new BigDecimal("50.00"), new BigDecimal("30.67"),
                new BigDecimal("36000"), new BigDecimal("6970000"), BigDecimal.ZERO, "CUADRA", List.of());

        JournalFullWorkbookData fullData = new JournalFullWorkbookData(
                snapshot,
                List.of(new AssignedVendor("1001", "Carlos P.", "TITULAR", "DIA6", 1, "V-01")),
                null,
                List.of(carlos),
                null,
                ClosingInventorySnapshot.empty());

        byte[] bytes = exporter.export(fullData);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(8, wb.getNumberOfSheets());
            assertEquals("Notas y Observaciones", wb.getSheetAt(7).getSheetName());

            XSSFSheet stepsSheet = wb.getSheet("Pasos");
            assertEquals("Observaciones", stepsSheet.getRow(0).getCell(4).getStringCellValue());
            assertEquals("Turno de apoyo inició a las 10:00.", stepsSheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("Se verificó bono en efectivo con recibo.", stepsSheet.getRow(2).getCell(4).getStringCellValue());

            XSSFSheet vendorSheet = wb.getSheet("Cierre Vendedores");
            var cashComment = vendorSheet.getRow(1).getCell(10).getCellComment();
            assertNotNull(cashComment);
            assertTrue(cashComment.getString().getString().contains("[Parciales] Sobre #2 con novedad de billete."));
            assertTrue(cashComment.getString().getString().contains("[Pico] Cambio entregado en monedas."));

            var credibancoComment = vendorSheet.getRow(1).getCell(12).getCellComment();
            assertNotNull(credibancoComment);
            assertEquals("Pasado en datafono alterno.", credibancoComment.getString().getString());

            XSSFSheet notesSheet = wb.getSheet("Notas y Observaciones");
            boolean foundVendorNoteRow = false;
            for (int r = 0; r <= notesSheet.getLastRowNum(); r++) {
                var row = notesSheet.getRow(r);
                if (row != null && row.getCell(1) != null
                        && "Carlos P. (1001)".equals(row.getCell(1).getStringCellValue())
                        && "Credibanco".equals(row.getCell(2).getStringCellValue())) {
                    assertEquals("Pasado en datafono alterno.", row.getCell(3).getStringCellValue());
                    foundVendorNoteRow = true;
                }
            }
            assertTrue(foundVendorNoteRow);
        }
    }
}
