package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpenseItemInfo;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalFullWorkbookData;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import com.edu.unicauca.gasstation.backend.shifts.catalog.CustomShiftCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class JournalWorkbookExporter {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(JournalSnapshot journal) throws IOException {
        return export(JournalFullWorkbookData.fromSnapshot(journal));
    }

    public byte[] export(JournalFullWorkbookData data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            createSummarySheet(workbook, data, styles);
            createStepsSheet(workbook, data.journal(), styles);
            createStaffSheet(workbook, data.staff(), styles);
            createVendorClosesSheet(workbook, data.vendorCloses(), styles);
            createProductsSheet(workbook, data, styles);
            createCashBalanceSheet(workbook, data.cashBalance(), styles);
            createInventorySheet(workbook, data.inventory(), styles);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void createSummarySheet(Workbook workbook, JournalFullWorkbookData data, Styles styles) {
        JournalSnapshot journal = data.journal();
        Sheet sheet = workbook.createSheet("Jornada");
        Row header = sheet.createRow(0);
        writeHeaderCell(header, 0, "Campo", styles);
        writeHeaderCell(header, 1, "Valor", styles);

        writeTextRow(sheet, 1, "ID de jornada", journal.id().toString(), styles);
        writeTextRow(sheet, 2, "Fecha", journal.date().toString(), styles);
        writeTextRow(sheet, 3, "Consecutivo", journal.consecutive().toString(), styles);
        writeTextRow(sheet, 4, "Estado", journal.status().name(), styles);
        writeTextRow(sheet, 5, "Creada el", format(journal.createdAt()), styles);

        int rowIdx = 7;
        DaySummary daySummary = data.daySummary();
        if (daySummary != null) {
            writeSectionRow(sheet, rowIdx++, "Resumen operativo del día", 2, styles);
            writeDecimalRow(sheet, rowIdx++, "Total galones vendidos", daySummary.totalFuelGallons(), styles.gallons, styles);
            writeDecimalRow(sheet, rowIdx++, "Lubricantes y canasta", daySummary.basketDominusAmount(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Efectivo declarado", daySummary.totalCash(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "G.A.", daySummary.totalGa(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Credibanco", daySummary.totalCredibanco(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "QR", daySummary.totalQr(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Puntos Colombia", daySummary.totalRedemptions(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Total entregado", daySummary.totalDelivered(), styles.currencyBold, styles);
            writeDecimalRow(sheet, rowIdx++, "Tirilla Dominus", daySummary.totalDominusTirilla(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Descuadre total", daySummary.totalDiscrepancy(), styles.currencyBold, styles);
            rowIdx++;
        }

        CashBalanceTableInfo cash = data.cashBalance();
        if (cash != null) {
            writeSectionRow(sheet, rowIdx++, "Saldo en caja y fondo de ahorro", 2, styles);
            writeDecimalRow(sheet, rowIdx++, "Saldo en caja", cash.cashBalance(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Saldo efectivo planilla", cash.planillaCashBalance(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Saldo acumulado Dominus", cash.dominusAccumulatedBalance(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Diferencia Dominus vs planilla", cash.difference(), styles.currencyBold, styles);
            writeDecimalRow(sheet, rowIdx++, "Fondo de ahorro acumulado", cash.savingsFund().accumulatedBalance(), styles.currency, styles);
            writeDecimalRow(sheet, rowIdx++, "Gastos administrativos del día", cash.expenses().total(), styles.currency, styles);
            rowIdx++;
        }

        ClosingInventorySnapshot inv = data.inventory();
        writeSectionRow(sheet, rowIdx++, "Cobertura e inventario", 2, styles);
        writeTextRow(sheet, rowIdx++, "Personal en turno", String.valueOf(data.staff().size()), styles);
        writeTextRow(sheet, rowIdx++, "Cierres de vendedor", String.valueOf(data.vendorCloses().size()), styles);
        writeTextRow(sheet, rowIdx++, "Variaciones de combustible", String.valueOf(inv.variations().size()), styles);
        writeTextRow(sheet, rowIdx++, "Alertas nuevas de autonomía", String.valueOf(inv.newAlerts().size()), styles);

        if (journal.observations() != null && !journal.observations().isBlank()) {
            rowIdx++;
            writeSectionRow(sheet, rowIdx++, "Observaciones de la jornada", 2, styles);
            writeTextRow(sheet, rowIdx, "Notas operativas", journal.observations(), styles);
        }

        finishSheet(sheet, 2, 1);
    }

    private static void createStepsSheet(Workbook workbook, JournalSnapshot journal, Styles styles) {
        Sheet sheet = workbook.createSheet("Pasos");
        Row header = sheet.createRow(0);
        writeHeaderCell(header, 0, "N.º paso", styles);
        writeHeaderCell(header, 1, "Nombre del paso", styles);
        writeHeaderCell(header, 2, "Estado", styles);
        writeHeaderCell(header, 3, "Completado el", styles);

        int rowNumber = 1;
        for (JournalSnapshot.JournalStepSnapshot step : journal.steps()) {
            Row row = sheet.createRow(rowNumber++);
            writeIntCell(row, 0, step.stepNumber(), styles.integer);
            writeTextCell(row, 1, step.name(), styles.text);
            writeTextCell(row, 2, step.status().name(), styles.text);
            writeTextCell(row, 3, format(step.completedAt()), styles.text);
        }
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rowNumber - 1), 0, 3));
        finishSheet(sheet, 4, 1);
    }

    private static void createStaffSheet(Workbook workbook, List<AssignedVendor> staff, Styles styles) {
        Sheet sheet = workbook.createSheet("Personal");
        String[] headers = {"Código", "Documento", "Vendedor", "Rol", "Turno", "Surtidor"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeHeaderCell(header, i, headers[i], styles);
        }

        int rowNumber = 1;
        if (staff.isEmpty()) {
            Row row = sheet.createRow(rowNumber++);
            writeTextCell(row, 0, "Sin personal registrado para esta jornada", styles.muted);
        } else {
            for (AssignedVendor member : staff) {
                Row row = sheet.createRow(rowNumber++);
                writeTextCell(row, 0, member.workerCode(), styles.text);
                writeTextCell(row, 1, member.document(), styles.text);
                writeTextCell(row, 2, member.name(), styles.text);
                writeTextCell(row, 3, member.role(), styles.text);
                writeTextCell(row, 4, CustomShiftCode.displayText(member.shiftName()), styles.text);
                writeTextCell(row, 5, member.pumpNumber() != null ? member.pumpNumber().toString() : "", styles.text);
            }
            sheet.setAutoFilter(new CellRangeAddress(0, rowNumber - 1, 0, headers.length - 1));
        }
        finishSheet(sheet, headers.length, 1);
    }

    private static void createVendorClosesSheet(
            Workbook workbook,
            List<VendorCloseSummary> vendorCloses,
            Styles styles) {
        Sheet sheet = workbook.createSheet("Cierre Vendedores");
        String[] headers = {
                "Vendedor", "Documento", "Turno", "Surtidor",
                "Gal. Corriente", "Gal. Diésel", "Gal. Extra", "Gal. Max Pro", "Total Galones",
                "Canasta / Lubricantes", "Efectivo", "G.A.", "Credibanco", "QR", "Puntos",
                "Total Entregado", "Tirilla Dominus", "Descuadre", "Estado"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeHeaderCell(header, i, headers[i], styles);
        }

        int rowNumber = 1;
        if (vendorCloses.isEmpty()) {
            Row row = sheet.createRow(rowNumber);
            writeTextCell(row, 0, "Sin cierres de vendedor registrados para esta jornada", styles.muted);
            finishSheet(sheet, headers.length, 1);
            return;
        }

        BigDecimal sumMotor = BigDecimal.ZERO;
        BigDecimal sumDiesel = BigDecimal.ZERO;
        BigDecimal sumExtra = BigDecimal.ZERO;
        BigDecimal sumMaxpro = BigDecimal.ZERO;
        BigDecimal sumGallons = BigDecimal.ZERO;
        BigDecimal sumBasket = BigDecimal.ZERO;
        BigDecimal sumCash = BigDecimal.ZERO;
        BigDecimal sumGa = BigDecimal.ZERO;
        BigDecimal sumCredibanco = BigDecimal.ZERO;
        BigDecimal sumQr = BigDecimal.ZERO;
        BigDecimal sumRedemptions = BigDecimal.ZERO;
        BigDecimal sumDelivered = BigDecimal.ZERO;
        BigDecimal sumTirilla = BigDecimal.ZERO;
        BigDecimal sumDiscrepancy = BigDecimal.ZERO;

        for (VendorCloseSummary close : vendorCloses) {
            BigDecimal motor = nz(close.gallonsMotor());
            BigDecimal diesel = nz(close.gallonsDiesel());
            BigDecimal extra = nz(close.gallonsExtra());
            BigDecimal maxpro = nz(close.gallonsMaxpro());
            BigDecimal totalGal = motor.add(diesel).add(extra).add(maxpro);

            sumMotor = sumMotor.add(motor);
            sumDiesel = sumDiesel.add(diesel);
            sumExtra = sumExtra.add(extra);
            sumMaxpro = sumMaxpro.add(maxpro);
            sumGallons = sumGallons.add(totalGal);
            sumBasket = sumBasket.add(nz(close.basketDominusAmount()));
            sumCash = sumCash.add(nz(close.cash()));
            sumGa = sumGa.add(nz(close.ga()));
            sumCredibanco = sumCredibanco.add(nz(close.credibanco()));
            sumQr = sumQr.add(nz(close.qr()));
            sumRedemptions = sumRedemptions.add(nz(close.redemptions()));
            sumDelivered = sumDelivered.add(nz(close.totalDelivered()));
            sumTirilla = sumTirilla.add(nz(close.dominusTirilla()));
            sumDiscrepancy = sumDiscrepancy.add(nz(close.realDiscrepancy()));

            Row row = sheet.createRow(rowNumber++);
            writeTextCell(row, 0, close.vendorName(), styles.text);
            writeTextCell(row, 1, close.vendorDocument(), styles.text);
            writeTextCell(row, 2, CustomShiftCode.displayText(close.shiftName()), styles.text);
            writeTextCell(row, 3, close.pumpNumber() != null ? close.pumpNumber().toString() : "", styles.text);
            writeDecimalCell(row, 4, motor, styles.gallons);
            writeDecimalCell(row, 5, diesel, styles.gallons);
            writeDecimalCell(row, 6, extra, styles.gallons);
            writeDecimalCell(row, 7, maxpro, styles.gallons);
            writeDecimalCell(row, 8, totalGal, styles.gallonsBold);
            writeDecimalCell(row, 9, nz(close.basketDominusAmount()), styles.currency);
            writeDecimalCell(row, 10, nz(close.cash()), styles.currency);
            writeDecimalCell(row, 11, nz(close.ga()), styles.currency);
            writeDecimalCell(row, 12, nz(close.credibanco()), styles.currency);
            writeDecimalCell(row, 13, nz(close.qr()), styles.currency);
            writeDecimalCell(row, 14, nz(close.redemptions()), styles.currency);
            writeDecimalCell(row, 15, nz(close.totalDelivered()), styles.currencyBold);
            writeDecimalCell(row, 16, nz(close.dominusTirilla()), styles.currency);
            writeDecimalCell(row, 17, nz(close.realDiscrepancy()), styles.currency);
            writeTextCell(row, 18, close.resultStatus(), styles.text);
        }

        Row totalRow = sheet.createRow(rowNumber);
        writeTextCell(totalRow, 0, "TOTAL", styles.totalLabel);
        writeTextCell(totalRow, 1, "", styles.totalLabel);
        writeTextCell(totalRow, 2, "", styles.totalLabel);
        writeTextCell(totalRow, 3, "", styles.totalLabel);
        writeDecimalCell(totalRow, 4, sumMotor, styles.totalGallons);
        writeDecimalCell(totalRow, 5, sumDiesel, styles.totalGallons);
        writeDecimalCell(totalRow, 6, sumExtra, styles.totalGallons);
        writeDecimalCell(totalRow, 7, sumMaxpro, styles.totalGallons);
        writeDecimalCell(totalRow, 8, sumGallons, styles.totalGallons);
        writeDecimalCell(totalRow, 9, sumBasket, styles.totalCurrency);
        writeDecimalCell(totalRow, 10, sumCash, styles.totalCurrency);
        writeDecimalCell(totalRow, 11, sumGa, styles.totalCurrency);
        writeDecimalCell(totalRow, 12, sumCredibanco, styles.totalCurrency);
        writeDecimalCell(totalRow, 13, sumQr, styles.totalCurrency);
        writeDecimalCell(totalRow, 14, sumRedemptions, styles.totalCurrency);
        writeDecimalCell(totalRow, 15, sumDelivered, styles.totalCurrency);
        writeDecimalCell(totalRow, 16, sumTirilla, styles.totalCurrency);
        writeDecimalCell(totalRow, 17, sumDiscrepancy, styles.totalCurrency);
        writeTextCell(totalRow, 18, "", styles.totalLabel);

        sheet.setAutoFilter(new CellRangeAddress(0, rowNumber - 1, 0, headers.length - 1));
        finishSheet(sheet, headers.length, 1);
    }

    private static void createProductsSheet(Workbook workbook, JournalFullWorkbookData data, Styles styles) {
        Sheet sheet = workbook.createSheet("Lubricantes y Otros");
        String[] headers = {"SKU", "Producto", "Categoría", "Unidades", "Precio Unitario", "Total"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeHeaderCell(header, i, headers[i], styles);
        }

        List<ProductSaleSummary> products = collectProducts(data);
        if (products.isEmpty()) {
            Row row = sheet.createRow(1);
            writeTextCell(row, 0, "Sin ventas de lubricantes o aditivos registradas", styles.muted);
            finishSheet(sheet, headers.length, 1);
            return;
        }

        int rowNumber = 1;
        int totalUnits = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ProductSaleSummary product : products) {
            totalUnits += product.unitsSold();
            totalAmount = totalAmount.add(nz(product.totalAmount()));

            Row row = sheet.createRow(rowNumber++);
            writeTextCell(row, 0, product.sku(), styles.text);
            writeTextCell(row, 1, product.productName(), styles.text);
            writeTextCell(row, 2, product.category(), styles.text);
            writeIntCell(row, 3, product.unitsSold(), styles.integer);
            writeDecimalCell(row, 4, nz(product.unitPrice()), styles.currency);
            writeDecimalCell(row, 5, nz(product.totalAmount()), styles.currency);
        }

        Row totalRow = sheet.createRow(rowNumber);
        writeTextCell(totalRow, 0, "TOTAL", styles.totalLabel);
        writeTextCell(totalRow, 1, "", styles.totalLabel);
        writeTextCell(totalRow, 2, "", styles.totalLabel);
        writeIntCell(totalRow, 3, totalUnits, styles.totalInteger);
        writeTextCell(totalRow, 4, "", styles.totalLabel);
        writeDecimalCell(totalRow, 5, totalAmount, styles.totalCurrency);

        sheet.setAutoFilter(new CellRangeAddress(0, rowNumber - 1, 0, headers.length - 1));
        finishSheet(sheet, headers.length, 1);
    }

    private static List<ProductSaleSummary> collectProducts(JournalFullWorkbookData data) {
        if (data.daySummary() != null && !data.daySummary().products().isEmpty()) {
            return data.daySummary().products();
        }
        Map<String, ProductSaleSummary> merged = new LinkedHashMap<>();
        for (VendorCloseSummary close : data.vendorCloses()) {
            if (close.products() == null) {
                continue;
            }
            for (ProductSaleSummary item : close.products()) {
                String key = item.sku() != null ? item.sku() : item.productName();
                merged.merge(key, item, (a, b) -> new ProductSaleSummary(
                        a.sku(),
                        a.productName(),
                        a.category(),
                        a.unitsSold() + b.unitsSold(),
                        a.unitPrice(),
                        nz(a.totalAmount()).add(nz(b.totalAmount()))));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private static void createCashBalanceSheet(Workbook workbook, CashBalanceTableInfo cash, Styles styles) {
        Sheet sheet = workbook.createSheet("Saldo en Caja");
        Row header = sheet.createRow(0);
        writeHeaderCell(header, 0, "Concepto", styles);
        writeHeaderCell(header, 1, "Valor", styles);

        if (cash == null) {
            Row row = sheet.createRow(1);
            writeTextCell(row, 0, "Sin registro de saldo en caja para esta jornada", styles.muted);
            finishSheet(sheet, 2, 1);
            return;
        }

        int rowIdx = 1;
        writeSectionRow(sheet, rowIdx++, "Fondo de ahorro", 2, styles);
        writeDecimalRow(sheet, rowIdx++, "Saldo anterior", cash.savingsFund().previousBalance(), styles.currency, styles);
        writeDecimalRow(sheet, rowIdx++, "Aporte del día", cash.savingsFund().contribution(), styles.currency, styles);
        writeDecimalRow(sheet, rowIdx++, "Pagos realizados", cash.savingsFund().payments(), styles.currency, styles);
        writeDecimalRow(sheet, rowIdx++, "Saldo acumulado", cash.savingsFund().accumulatedBalance(), styles.currencyBold, styles);
        rowIdx++;

        writeSectionRow(sheet, rowIdx++, "Cuadre de efectivo y Dominus", 2, styles);
        writeDecimalRow(sheet, rowIdx++, "Saldo en caja", cash.cashBalance(), styles.currency, styles);
        writeDecimalRow(sheet, rowIdx++, "Efectivo del día (vendedores)", cash.dailyCash(), styles.currency, styles);
        writeDecimalRow(sheet, rowIdx++, "Saldo efectivo en planilla", cash.planillaCashBalance(), styles.currencyBold, styles);
        writeDecimalRow(sheet, rowIdx++, "Saldo acumulado Dominus", cash.dominusAccumulatedBalance(), styles.currency, styles);
        writeDecimalRow(sheet, rowIdx++, "Diferencia (Dominus - Planilla)", cash.difference(), styles.currencyBold, styles);
        rowIdx++;

        writeSectionRow(sheet, rowIdx++, "Gastos administrativos del día", 3, styles);
        Row expHeader = sheet.createRow(rowIdx++);
        writeHeaderCell(expHeader, 0, "Fecha", styles);
        writeHeaderCell(expHeader, 1, "Descripción", styles);
        writeHeaderCell(expHeader, 2, "Monto", styles);

        if (cash.expenses().items().isEmpty()) {
            Row emptyRow = sheet.createRow(rowIdx++);
            writeTextCell(emptyRow, 0, "Sin gastos administrativos registrados", styles.muted);
        } else {
            for (ExpenseItemInfo item : cash.expenses().items()) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, item.date() != null ? item.date().toString() : "", styles.text);
                writeTextCell(row, 1, item.description(), styles.text);
                writeDecimalCell(row, 2, nz(item.amount()), styles.currency);
            }
        }
        Row expTotal = sheet.createRow(rowIdx);
        writeTextCell(expTotal, 0, "TOTAL GASTOS", styles.totalLabel);
        writeTextCell(expTotal, 1, "", styles.totalLabel);
        writeDecimalCell(expTotal, 2, cash.expenses().total(), styles.totalCurrency);

        finishSheet(sheet, 3, 1);
    }

    private static void createInventorySheet(Workbook workbook, ClosingInventorySnapshot inventory, Styles styles) {
        Sheet sheet = workbook.createSheet("Inventario y Variación");
        String[] varHeaders = {
                "Combustible", "Inicial (gal)", "Entradas (gal)", "Vendido (gal)",
                "Teórico (gal)", "Medido (gal)", "Variación (gal)", "Tolerancia (gal)", "Estado"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < varHeaders.length; i++) {
            writeHeaderCell(header, i, varHeaders[i], styles);
        }

        int rowIdx = 1;
        if (inventory.variations().isEmpty()) {
            Row row = sheet.createRow(rowIdx++);
            writeTextCell(row, 0, "Sin variaciones de combustible registradas para esta jornada", styles.muted);
        } else {
            for (FuelVariationSummary item : inventory.variations()) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, item.fuelType(), styles.text);
                writeDecimalCell(row, 1, nz(item.initialGallons()), styles.gallons);
                writeDecimalCell(row, 2, nz(item.entriesGallons()), styles.gallons);
                writeDecimalCell(row, 3, nz(item.soldGallons()), styles.gallons);
                writeDecimalCell(row, 4, nz(item.theoreticalGallons()), styles.gallons);
                writeDecimalCell(row, 5, nz(item.measuredGallons()), styles.gallons);
                writeDecimalCell(row, 6, nz(item.varianceGallons()), styles.gallonsBold);
                writeDecimalCell(row, 7, nz(item.toleranceGallons()), styles.gallons);
                writeTextCell(row, 8, item.status(), styles.text);
            }
        }

        rowIdx++;
        writeSectionRow(sheet, rowIdx++, "Alertas de autonomía generadas", 6, styles);
        String[] alertHeaders = {"Tipo", "Combustible", "Nivel", "Estado", "Días de autonomía", "Fecha"};
        Row alertHeader = sheet.createRow(rowIdx++);
        for (int i = 0; i < alertHeaders.length; i++) {
            writeHeaderCell(alertHeader, i, alertHeaders[i], styles);
        }

        if (inventory.newAlerts().isEmpty()) {
            Row row = sheet.createRow(rowIdx);
            writeTextCell(row, 0, "Sin alertas nuevas de autonomía en esta jornada", styles.muted);
        } else {
            for (InventoryAlertSummary alert : inventory.newAlerts()) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, alert.alertType(), styles.text);
                writeTextCell(row, 1, alert.fuelType(), styles.text);
                writeTextCell(row, 2, alert.level(), styles.text);
                writeTextCell(row, 3, alert.status(), styles.text);
                writeDecimalCell(row, 4, alert.autonomyDays(), styles.gallons);
                writeTextCell(row, 5, alert.openedOn() != null ? alert.openedOn().toString() : "", styles.text);
            }
        }

        finishSheet(sheet, varHeaders.length, 1);
    }

    private static void writeTextRow(Sheet sheet, int rowNumber, String label, String value, Styles styles) {
        Row row = sheet.createRow(rowNumber);
        writeTextCell(row, 0, label, styles.text);
        writeTextCell(row, 1, value, styles.text);
    }

    private static void writeDecimalRow(
            Sheet sheet,
            int rowNumber,
            String label,
            BigDecimal value,
            CellStyle valueStyle,
            Styles styles) {
        Row row = sheet.createRow(rowNumber);
        writeTextCell(row, 0, label, styles.text);
        writeDecimalCell(row, 1, value, valueStyle);
    }

    private static void writeSectionRow(Sheet sheet, int rowNumber, String title, int spanCols, Styles styles) {
        Row row = sheet.createRow(rowNumber);
        for (int i = 0; i < spanCols; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(styles.section);
            if (i == 0) {
                cell.setCellValue(title);
            }
        }
    }

    private static void writeHeaderCell(Row row, int column, String value, Styles styles) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(styles.header);
    }

    private static void writeTextCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void writeIntCell(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void writeDecimalCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("—");
        } else {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static void finishSheet(Sheet sheet, int columns, int frozenRows) {
        sheet.createFreezePane(0, frozenRows);
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(Math.max(sheet.getColumnWidth(column) + 512, 12 * 256), 60 * 256));
        }
    }

    private static String format(java.time.LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMAT);
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle section;
        final CellStyle text;
        final CellStyle muted;
        final CellStyle integer;
        final CellStyle currency;
        final CellStyle currencyBold;
        final CellStyle gallons;
        final CellStyle gallonsBold;
        final CellStyle totalLabel;
        final CellStyle totalInteger;
        final CellStyle totalCurrency;
        final CellStyle totalGallons;

        Styles(Workbook workbook) {
            DataFormat dataFormat = workbook.createDataFormat();
            short copFmt = dataFormat.getFormat("$#,##0;-$#,##0;$0");
            short galFmt = dataFormat.getFormat("#,##0.00");
            short intFmt = dataFormat.getFormat("#,##0");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            Font mutedFont = workbook.createFont();
            mutedFont.setItalic(true);
            mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            section = workbook.createCellStyle();
            section.setFont(boldFont);
            section.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            section.setBorderBottom(BorderStyle.THIN);

            text = bordered(workbook);

            muted = workbook.createCellStyle();
            muted.setFont(mutedFont);

            integer = bordered(workbook);
            integer.setAlignment(HorizontalAlignment.RIGHT);
            integer.setDataFormat(intFmt);

            currency = bordered(workbook);
            currency.setAlignment(HorizontalAlignment.RIGHT);
            currency.setDataFormat(copFmt);

            currencyBold = bordered(workbook);
            currencyBold.setFont(boldFont);
            currencyBold.setAlignment(HorizontalAlignment.RIGHT);
            currencyBold.setDataFormat(copFmt);

            gallons = bordered(workbook);
            gallons.setAlignment(HorizontalAlignment.RIGHT);
            gallons.setDataFormat(galFmt);

            gallonsBold = bordered(workbook);
            gallonsBold.setFont(boldFont);
            gallonsBold.setAlignment(HorizontalAlignment.RIGHT);
            gallonsBold.setDataFormat(galFmt);

            totalLabel = totalStyle(workbook, boldFont, null, HorizontalAlignment.LEFT);
            totalInteger = totalStyle(workbook, boldFont, intFmt, HorizontalAlignment.RIGHT);
            totalCurrency = totalStyle(workbook, boldFont, copFmt, HorizontalAlignment.RIGHT);
            totalGallons = totalStyle(workbook, boldFont, galFmt, HorizontalAlignment.RIGHT);
        }

        private static CellStyle bordered(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderBottom(BorderStyle.HAIR);
            style.setBorderTop(BorderStyle.HAIR);
            style.setBorderLeft(BorderStyle.HAIR);
            style.setBorderRight(BorderStyle.HAIR);
            return style;
        }

        private static CellStyle totalStyle(
                Workbook workbook,
                Font boldFont,
                Short format,
                HorizontalAlignment alignment) {
            CellStyle style = workbook.createCellStyle();
            style.setFont(boldFont);
            style.setAlignment(alignment);
            style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.DOUBLE);
            if (format != null) {
                style.setDataFormat(format);
            }
            return style;
        }
    }
}
