package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles;
import com.edu.unicauca.gasstation.backend.excel.ExcelWorkbookStyles.Styles;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.shifts.catalog.CustomShiftCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Builds the two Excel files of the end-of-day step: PLANILLA VENTAS and LUBRICANTES Y VARIOS.
 * It only writes the data it receives from SellerCloseQuery. The only sums it makes are the
 * gallons total and the additive units of each vendor row.
 * All the layout lives here, so changing the format means changing only this class.
 * Money columns use the currency format and the vendor result is painted green, amber or red.
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
        try (XSSFWorkbook objWorkbook = new XSSFWorkbook(); ByteArrayOutputStream objOutput = new ByteArrayOutputStream()) {
            Styles objStyles = Styles.of(objWorkbook);
            Sheet objSheet = objWorkbook.createSheet("PLANILLA VENTAS");
            int varRow = ExcelWorkbookStyles.writeHeading(objSheet, "PLANILLA VENTAS", prmDay.date(), SALES_HEADERS, objStyles);
            for (VendorCloseSummary objVendor : prmVendors) {
                writeVendorRow(objSheet.createRow(varRow++), objVendor, objStyles);
            }
            writeDayTotalRow(objSheet.createRow(varRow), prmDay, objStyles);
            ExcelWorkbookStyles.finish(objSheet, SALES_HEADERS.length);
            objWorkbook.write(objOutput);
            return objOutput.toByteArray();
        }
    }

    /** Ensures every cell in the data area has a thin border on all sides, even when the value is null.
     *  PLANILLA cols 1–3 and 18 must never have unstyled gaps; LUBRICANTES cols 0, 2, 3, 4 as well.
     */
    private static void ensureBorders(Row prmRow, int prmLastCell) {
        for (int varCol = 0; varCol <= prmLastCell; varCol++) {
            var objCell = prmRow.getCell(varCol);
            if (objCell == null) {
                objCell = prmRow.createCell(varCol);
                objCell.setCellValue("");
            }
            objCell.setCellStyle(objCell.getCellStyle());
        }
    }

    /** Builds the LUBRICANTES Y VARIOS file: products sold in the day, totals and the basket summary. */
    public byte[] exportLubricants(DaySummary prmDay) throws IOException {
        try (XSSFWorkbook objWorkbook = new XSSFWorkbook(); ByteArrayOutputStream objOutput = new ByteArrayOutputStream()) {
            Styles objStyles = Styles.of(objWorkbook);
            writeProducts(objWorkbook.createSheet("LUBRICANTES Y VARIOS"), prmDay, objStyles);
            objWorkbook.write(objOutput);
            return objOutput.toByteArray();
        }
    }

    private static void writeVendorRow(Row prmRow, VendorCloseSummary prmVendor, Styles prmStyles) {
        ExcelWorkbookStyles.text(prmRow, 0, prmVendor.vendorName(), prmStyles.text());
        ExcelWorkbookStyles.text(prmRow, 1, prmVendor.vendorDocument(), prmStyles.text());
        ExcelWorkbookStyles.text(prmRow, 2, CustomShiftCode.displayText(prmVendor.shiftName()), prmStyles.text());
        if (prmVendor.pumpNumber() != null) {
            ExcelWorkbookStyles.whole(prmRow, 3, prmVendor.pumpNumber(), prmStyles.integer());
        } else {
            ExcelWorkbookStyles.number(prmRow, 3, null, prmStyles.integer());
        }
        ExcelWorkbookStyles.number(prmRow, 4, prmVendor.gallonsMotor(), prmStyles.number());
        ExcelWorkbookStyles.number(prmRow, 5, prmVendor.gallonsDiesel(), prmStyles.number());
        ExcelWorkbookStyles.number(prmRow, 6, prmVendor.gallonsExtra(), prmStyles.number());
        ExcelWorkbookStyles.number(prmRow, 7, prmVendor.gallonsMaxpro(), prmStyles.number());
        ExcelWorkbookStyles.number(prmRow, 8, sum(prmVendor.gallonsMotor(), prmVendor.gallonsDiesel(),
                prmVendor.gallonsExtra(), prmVendor.gallonsMaxpro()), prmStyles.number());
        ExcelWorkbookStyles.whole(prmRow, 9, additiveUnits(prmVendor.products()), prmStyles.integer());
        ExcelWorkbookStyles.number(prmRow, 10, prmVendor.cash(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 11, prmVendor.ga(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 12, prmVendor.credibanco(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 13, prmVendor.qr(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 14, prmVendor.redemptions(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 15, prmVendor.totalDelivered(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 16, prmVendor.dominusTirilla(), prmStyles.money());
        ExcelWorkbookStyles.number(prmRow, 17, prmVendor.realDiscrepancy(),
                ExcelWorkbookStyles.differenceStyle(prmVendor.resultStatus(), prmStyles));
        ExcelWorkbookStyles.text(prmRow, 18, prmVendor.resultStatus(),
                ExcelWorkbookStyles.resultStyle(prmVendor.resultStatus(), prmStyles));
        // Ensure all cells have a border so there are no unstyled gaps (PLANILLA cols 1–3 and 18;
        // the row's last used column is 18, so we border up to column 18).
        ensureBorders(prmRow, 18);
    }

    private static void writeDayTotalRow(Row prmRow, DaySummary prmDay, Styles prmStyles) {
        ExcelWorkbookStyles.text(prmRow, 0, "TOTAL DEL DÍA", prmStyles.totalText());
        ExcelWorkbookStyles.text(prmRow, 1, "", prmStyles.totalText());
        ExcelWorkbookStyles.text(prmRow, 2, "", prmStyles.totalText());
        ExcelWorkbookStyles.number(prmRow, 3, null, prmStyles.totalInteger());
        ExcelWorkbookStyles.number(prmRow, 4, prmDay.gallonsMotor(), prmStyles.totalNumber());
        ExcelWorkbookStyles.number(prmRow, 5, prmDay.gallonsDiesel(), prmStyles.totalNumber());
        ExcelWorkbookStyles.number(prmRow, 6, prmDay.gallonsExtra(), prmStyles.totalNumber());
        ExcelWorkbookStyles.number(prmRow, 7, prmDay.gallonsMaxpro(), prmStyles.totalNumber());
        ExcelWorkbookStyles.number(prmRow, 8, prmDay.totalFuelGallons(), prmStyles.totalNumber());
        ExcelWorkbookStyles.whole(prmRow, 9, additiveUnits(prmDay.products()), prmStyles.totalInteger());
        ExcelWorkbookStyles.number(prmRow, 10, prmDay.totalCash(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 11, prmDay.totalGa(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 12, prmDay.totalCredibanco(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 13, prmDay.totalQr(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 14, prmDay.totalRedemptions(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 15, prmDay.totalDelivered(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 16, prmDay.totalDominusTirilla(), prmStyles.totalMoney());
        ExcelWorkbookStyles.number(prmRow, 17, prmDay.totalDiscrepancy(), prmStyles.totalMoney());
        ExcelWorkbookStyles.text(prmRow, 18, "", prmStyles.totalText());
        // Ensure all cells have a border so there are no unstyled gaps on the total row (PLANILLA cols 1–3 and 18).
        ensureBorders(prmRow, 18);
    }

    private static void writeProducts(Sheet prmSheet, DaySummary prmDay, Styles prmStyles) {
        int varRow = ExcelWorkbookStyles.writeHeading(prmSheet, "LUBRICANTES Y VARIOS", prmDay.date(),
                PRODUCT_HEADERS, prmStyles);
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
            ExcelWorkbookStyles.text(objRow, 0, objProduct.sku(), prmStyles.text());
            ExcelWorkbookStyles.text(objRow, 1, objProduct.productName(), prmStyles.text());
            ExcelWorkbookStyles.text(objRow, 2, varGroup, prmStyles.text());
            ExcelWorkbookStyles.number(objRow, 3, objProduct.unitPrice(), prmStyles.money());
            ExcelWorkbookStyles.whole(objRow, 4, objProduct.unitsSold(), prmStyles.integer());
            ExcelWorkbookStyles.number(objRow, 5, objProduct.totalAmount(), prmStyles.money());
            varTotalUnits += objProduct.unitsSold();
            objTotalAmount = objTotalAmount.add(zeroIfNull(objProduct.totalAmount()));
            objGroupTotals.merge(varGroup, zeroIfNull(objProduct.totalAmount()), BigDecimal::add);
        }
        Row objTotals = prmSheet.createRow(varRow++);
        ExcelWorkbookStyles.text(objTotals, 0, "", prmStyles.totalText());
        ExcelWorkbookStyles.text(objTotals, 1, "TOTALES", prmStyles.totalText());
        ExcelWorkbookStyles.text(objTotals, 2, "", prmStyles.totalText());
        ExcelWorkbookStyles.number(objTotals, 3, null, prmStyles.totalMoney());
        ExcelWorkbookStyles.whole(objTotals, 4, varTotalUnits, prmStyles.totalInteger());
        ExcelWorkbookStyles.number(objTotals, 5, objTotalAmount, prmStyles.totalMoney());
        varRow++;
        for (Map.Entry<String, BigDecimal> objEntry : objGroupTotals.entrySet()) {
            writeBasketRow(prmSheet.createRow(varRow++), "VENTA " + objEntry.getKey(), objEntry.getValue(), prmStyles);
        }
        writeBasketRow(prmSheet.createRow(varRow), "VENTA TOTAL CANASTA", objTotalAmount, prmStyles);
        ExcelWorkbookStyles.finish(prmSheet, PRODUCT_HEADERS.length);
    }

    /**
     * Basket summary row: same green band as the totals but without the double top border, so the
     * four rows of the summary read as one block instead of four separate totals.
     */
    private static void writeBasketRow(Row prmRow, String prmLabel, BigDecimal prmAmount, Styles prmStyles) {
        ExcelWorkbookStyles.text(prmRow, 0, "", prmStyles.subtotalText());
        ExcelWorkbookStyles.text(prmRow, 1, prmLabel, prmStyles.subtotalText());
        ExcelWorkbookStyles.text(prmRow, 2, "", prmStyles.subtotalText());
        ExcelWorkbookStyles.number(prmRow, 3, null, prmStyles.subtotalMoney());
        ExcelWorkbookStyles.number(prmRow, 4, null, prmStyles.subtotalInteger());
        ExcelWorkbookStyles.number(prmRow, 5, prmAmount, prmStyles.subtotalMoney());
        // Ensure all cells have a border so there are no unstyled gaps (LUBRICANTES cols 0, 2, 3, 4).
        ensureBorders(prmRow, 5);
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
}
