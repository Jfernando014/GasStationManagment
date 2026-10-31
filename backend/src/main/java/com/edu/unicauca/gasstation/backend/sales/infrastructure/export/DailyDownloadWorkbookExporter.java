package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Builds the two Excel files of the end-of-day step: PLANILLA VENTAS and LUBRICANTES Y VARIOS.
 * It only writes the data it receives from SellerCloseQuery. The only sums it makes are the
 * gallons total and the additive units of each vendor row.
 * All the layout lives here, so changing the format means changing only this class.
 *
 * @author javiersolanop777
 */
@Component
public class DailyDownloadWorkbookExporter {

    /** Product code of "Bolsas para Combustible". It is the only product of the group MERCANCIA VARIA. */
    static final String BAGS_SKU = "76001";

    private static final String GROUP_OILS = "ACEITES";
    private static final String GROUP_MISC = "MERCANCIA VARIA";
    private static final String GROUP_ADDITIVES = "ADITIVOS Y OTROS";
    private static final List<String> GROUPS = List.of(GROUP_OILS, GROUP_MISC, GROUP_ADDITIVES);

    private static final String[] SALES_HEADERS = {
        "Vendedor", "Documento", "Turno", "Bomba", "Galones Motor", "Galones Diesel", "Galones Extra",
        "Galones Maxpro", "Total galones", "Unidades aditivos", "Efectivo", "GA", "Credibanco", "QR",
        "Redenciones", "Total entregado", "Tirilla Dominus", "Diferencia", "Resultado"};
    private static final String[] PRODUCT_HEADERS = {
        "Código", "Producto", "Grupo", "Precio unidad", "Unidades", "Valor venta"};

    /** Builds the PLANILLA VENTAS file: one row per vendor and one total row for the day. */
    public byte[] exportSalesSheet(DaySummary prmDay, List<VendorCloseSummary> prmVendors) throws IOException {
        try (Workbook objWorkbook = new XSSFWorkbook(); ByteArrayOutputStream objOutput = new ByteArrayOutputStream()) {
            Styles objStyles = Styles.of(objWorkbook);
            Sheet objSheet = objWorkbook.createSheet("PLANILLA VENTAS");
            int varRow = writeHeading(objSheet, "PLANILLA VENTAS", prmDay.date(), SALES_HEADERS, objStyles);
            for (VendorCloseSummary objVendor : prmVendors) {
                writeVendorRow(objSheet.createRow(varRow++), objVendor, objStyles);
            }
            writeDayTotalRow(objSheet.createRow(varRow), prmDay, objStyles);
            finish(objSheet, SALES_HEADERS.length);
            objWorkbook.write(objOutput);
            return objOutput.toByteArray();
        }
    }

    /** Builds the LUBRICANTES Y VARIOS file: products sold in the day, totals and the basket summary. */
    public byte[] exportLubricants(DaySummary prmDay) throws IOException {
        try (Workbook objWorkbook = new XSSFWorkbook(); ByteArrayOutputStream objOutput = new ByteArrayOutputStream()) {
            Styles objStyles = Styles.of(objWorkbook);
            writeProducts(objWorkbook.createSheet("LUBRICANTES Y VARIOS"), prmDay, objStyles);
            objWorkbook.write(objOutput);
            return objOutput.toByteArray();
        }
    }

    private static void writeVendorRow(Row prmRow, VendorCloseSummary prmVendor, Styles prmStyles) {
        text(prmRow, 0, prmVendor.vendorName(), prmStyles.atrText());
        text(prmRow, 1, prmVendor.vendorDocument(), prmStyles.atrText());
        text(prmRow, 2, prmVendor.shiftName(), prmStyles.atrText());
        if (prmVendor.pumpNumber() != null) {
            whole(prmRow, 3, prmVendor.pumpNumber(), prmStyles.atrInteger());
        }
        number(prmRow, 4, prmVendor.gallonsMotor(), prmStyles.atrNumber());
        number(prmRow, 5, prmVendor.gallonsDiesel(), prmStyles.atrNumber());
        number(prmRow, 6, prmVendor.gallonsExtra(), prmStyles.atrNumber());
        number(prmRow, 7, prmVendor.gallonsMaxpro(), prmStyles.atrNumber());
        number(prmRow, 8, sum(prmVendor.gallonsMotor(), prmVendor.gallonsDiesel(),
                prmVendor.gallonsExtra(), prmVendor.gallonsMaxpro()), prmStyles.atrNumber());
        whole(prmRow, 9, additiveUnits(prmVendor.products()), prmStyles.atrInteger());
        number(prmRow, 10, prmVendor.cash(), prmStyles.atrNumber());
        number(prmRow, 11, prmVendor.ga(), prmStyles.atrNumber());
        number(prmRow, 12, prmVendor.credibanco(), prmStyles.atrNumber());
        number(prmRow, 13, prmVendor.qr(), prmStyles.atrNumber());
        number(prmRow, 14, prmVendor.redemptions(), prmStyles.atrNumber());
        number(prmRow, 15, prmVendor.totalDelivered(), prmStyles.atrNumber());
        number(prmRow, 16, prmVendor.dominusTirilla(), prmStyles.atrNumber());
        number(prmRow, 17, prmVendor.realDiscrepancy(), prmStyles.atrNumber());
        text(prmRow, 18, prmVendor.resultStatus(), prmStyles.atrText());
    }

    private static void writeDayTotalRow(Row prmRow, DaySummary prmDay, Styles prmStyles) {
        text(prmRow, 0, "TOTAL DEL DÍA", prmStyles.atrTotalText());
        number(prmRow, 4, prmDay.gallonsMotor(), prmStyles.atrTotalNumber());
        number(prmRow, 5, prmDay.gallonsDiesel(), prmStyles.atrTotalNumber());
        number(prmRow, 6, prmDay.gallonsExtra(), prmStyles.atrTotalNumber());
        number(prmRow, 7, prmDay.gallonsMaxpro(), prmStyles.atrTotalNumber());
        number(prmRow, 8, prmDay.totalFuelGallons(), prmStyles.atrTotalNumber());
        whole(prmRow, 9, additiveUnits(prmDay.products()), prmStyles.atrTotalInteger());
        number(prmRow, 10, prmDay.totalCash(), prmStyles.atrTotalNumber());
        number(prmRow, 11, prmDay.totalGa(), prmStyles.atrTotalNumber());
        number(prmRow, 12, prmDay.totalCredibanco(), prmStyles.atrTotalNumber());
        number(prmRow, 13, prmDay.totalQr(), prmStyles.atrTotalNumber());
        number(prmRow, 14, prmDay.totalRedemptions(), prmStyles.atrTotalNumber());
        number(prmRow, 15, prmDay.totalDelivered(), prmStyles.atrTotalNumber());
        number(prmRow, 16, prmDay.totalDominusTirilla(), prmStyles.atrTotalNumber());
        number(prmRow, 17, prmDay.totalDiscrepancy(), prmStyles.atrTotalNumber());
    }

    private static void writeProducts(Sheet prmSheet, DaySummary prmDay, Styles prmStyles) {
        int varRow = writeHeading(prmSheet, "LUBRICANTES Y VARIOS", prmDay.date(), PRODUCT_HEADERS, prmStyles);
        List<ProductSaleSummary> objProducts = prmDay.products().stream()
                .sorted(Comparator.comparingInt(
                                (ProductSaleSummary prmProduct) -> GROUPS.indexOf(groupOf(prmProduct.sku(), prmProduct.category())))
                        .thenComparing(prmProduct -> clean(prmProduct.productName()), String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<String, BigDecimal> objGroupTotals = new LinkedHashMap<>();
        for (String varGroup : GROUPS) {
            objGroupTotals.put(varGroup, BigDecimal.ZERO);
        }
        long varTotalUnits = 0;
        BigDecimal objTotalAmount = BigDecimal.ZERO;
        for (ProductSaleSummary objProduct : objProducts) {
            String varGroup = groupOf(objProduct.sku(), objProduct.category());
            Row objRow = prmSheet.createRow(varRow++);
            text(objRow, 0, objProduct.sku(), prmStyles.atrText());
            text(objRow, 1, objProduct.productName(), prmStyles.atrText());
            text(objRow, 2, varGroup, prmStyles.atrText());
            number(objRow, 3, objProduct.unitPrice(), prmStyles.atrNumber());
            whole(objRow, 4, objProduct.unitsSold(), prmStyles.atrInteger());
            number(objRow, 5, objProduct.totalAmount(), prmStyles.atrNumber());
            varTotalUnits += objProduct.unitsSold();
            objTotalAmount = objTotalAmount.add(zeroIfNull(objProduct.totalAmount()));
            objGroupTotals.merge(varGroup, zeroIfNull(objProduct.totalAmount()), BigDecimal::add);
        }
        Row objTotals = prmSheet.createRow(varRow++);
        text(objTotals, 1, "TOTALES", prmStyles.atrTotalText());
        whole(objTotals, 4, varTotalUnits, prmStyles.atrTotalInteger());
        number(objTotals, 5, objTotalAmount, prmStyles.atrTotalNumber());
        varRow++;
        for (Map.Entry<String, BigDecimal> objEntry : objGroupTotals.entrySet()) {
            Row objSummary = prmSheet.createRow(varRow++);
            text(objSummary, 1, "VENTA " + objEntry.getKey(), prmStyles.atrTotalText());
            number(objSummary, 5, objEntry.getValue(), prmStyles.atrTotalNumber());
        }
        Row objBasket = prmSheet.createRow(varRow);
        text(objBasket, 1, "VENTA TOTAL CANASTA", prmStyles.atrTotalText());
        number(objBasket, 5, objTotalAmount, prmStyles.atrTotalNumber());
        finish(prmSheet, PRODUCT_HEADERS.length);
    }

    /**
     * Group of the client summary: the bags are MERCANCIA VARIA, lubricants are ACEITES and
     * everything else (additives, coolant, brake fluid, water, others) is ADITIVOS Y OTROS.
     */
    private static String groupOf(String prmSku, String prmCategory) {
        if (BAGS_SKU.equals(prmSku)) {
            return GROUP_MISC;
        }
        if (prmCategory != null && "LUBRICANTE".equalsIgnoreCase(prmCategory.trim())) {
            return GROUP_OILS;
        }
        return GROUP_ADDITIVES;
    }

    private static long additiveUnits(List<ProductSaleSummary> prmProducts) {
        long varUnits = 0;
        for (ProductSaleSummary objProduct : prmProducts) {
            if (objProduct.category() != null && "ADITIVO".equalsIgnoreCase(objProduct.category().trim())) {
                varUnits += objProduct.unitsSold();
            }
        }
        return varUnits;
    }

    /** Writes title (row 0), date (row 1) and headers (row 3). Returns the first data row index. */
    private static int writeHeading(Sheet prmSheet, String prmTitle, LocalDate prmDate,
                                    String[] prmHeaders, Styles prmStyles) {
        text(prmSheet.createRow(0), 0, prmTitle, prmStyles.atrTitle());
        text(prmSheet.createRow(1), 0, "Fecha: " + prmDate, prmStyles.atrText());
        Row objHeader = prmSheet.createRow(3);
        for (int varColumn = 0; varColumn < prmHeaders.length; varColumn++) {
            text(objHeader, varColumn, prmHeaders[varColumn], prmStyles.atrHeader());
        }
        return 4;
    }

    private static void finish(Sheet prmSheet, int prmColumns) {
        prmSheet.createFreezePane(0, 4);
        for (int varColumn = 0; varColumn < prmColumns; varColumn++) {
            prmSheet.autoSizeColumn(varColumn);
            prmSheet.setColumnWidth(varColumn, Math.min(prmSheet.getColumnWidth(varColumn) + 512, 60 * 256));
        }
    }

    private static void text(Row prmRow, int prmColumn, String prmValue, CellStyle prmStyle) {
        Cell objCell = prmRow.createCell(prmColumn);
        objCell.setCellValue(prmValue == null ? "" : prmValue);
        objCell.setCellStyle(prmStyle);
    }

    private static void number(Row prmRow, int prmColumn, BigDecimal prmValue, CellStyle prmStyle) {
        Cell objCell = prmRow.createCell(prmColumn);
        objCell.setCellValue(prmValue == null ? 0 : prmValue.doubleValue());
        objCell.setCellStyle(prmStyle);
    }

    private static void whole(Row prmRow, int prmColumn, long prmValue, CellStyle prmStyle) {
        Cell objCell = prmRow.createCell(prmColumn);
        objCell.setCellValue(prmValue);
        objCell.setCellStyle(prmStyle);
    }

    private static BigDecimal sum(BigDecimal... prmValues) {
        BigDecimal objTotal = BigDecimal.ZERO;
        for (BigDecimal objValue : prmValues) {
            objTotal = objTotal.add(zeroIfNull(objValue));
        }
        return objTotal;
    }

    private static BigDecimal zeroIfNull(BigDecimal prmValue) {
        return prmValue == null ? BigDecimal.ZERO : prmValue;
    }

    private static String clean(String prmValue) {
        return prmValue == null ? "" : prmValue;
    }

    /** Cell styles shared by the sheets of one workbook. */
    private record Styles(CellStyle atrTitle, CellStyle atrHeader, CellStyle atrText, CellStyle atrNumber,
                          CellStyle atrInteger, CellStyle atrTotalText, CellStyle atrTotalNumber,
                          CellStyle atrTotalInteger) {

        static Styles of(Workbook prmWorkbook) {
            Font objBold = prmWorkbook.createFont();
            objBold.setBold(true);
            Font objBig = prmWorkbook.createFont();
            objBig.setBold(true);
            objBig.setFontHeightInPoints((short) 14);
            short varMoney = prmWorkbook.createDataFormat().getFormat("#,##0.00");
            short varWhole = prmWorkbook.createDataFormat().getFormat("#,##0");

            CellStyle objHeader = style(prmWorkbook, objBold, (short) 0);
            objHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            objHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            objHeader.setBorderBottom(BorderStyle.THIN);

            return new Styles(
                    style(prmWorkbook, objBig, (short) 0),
                    objHeader,
                    style(prmWorkbook, null, (short) 0),
                    style(prmWorkbook, null, varMoney),
                    style(prmWorkbook, null, varWhole),
                    style(prmWorkbook, objBold, (short) 0),
                    style(prmWorkbook, objBold, varMoney),
                    style(prmWorkbook, objBold, varWhole));
        }

        private static CellStyle style(Workbook prmWorkbook, Font prmFont, short prmFormat) {
            CellStyle objStyle = prmWorkbook.createCellStyle();
            if (prmFont != null) {
                objStyle.setFont(prmFont);
            }
            if (prmFormat != 0) {
                objStyle.setDataFormat(prmFormat);
            }
            return objStyle;
        }
    }
}
