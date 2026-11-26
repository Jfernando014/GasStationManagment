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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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

    private static final String RESULT_BALANCED = "CUADRA";
    private static final String RESULT_SHORTFALL = "FALTA";

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
        CellStyle objDifference = differenceStyle(prmVendor.resultStatus(), prmStyles);
        text(prmRow, 0, prmVendor.vendorName(), prmStyles.text());
        text(prmRow, 1, prmVendor.vendorDocument(), prmStyles.text());
        text(prmRow, 2, CustomShiftCode.displayText(prmVendor.shiftName()), prmStyles.text());
        if (prmVendor.pumpNumber() != null) {
            whole(prmRow, 3, prmVendor.pumpNumber(), prmStyles.integer());
        }
        number(prmRow, 4, prmVendor.gallonsMotor(), prmStyles.number());
        number(prmRow, 5, prmVendor.gallonsDiesel(), prmStyles.number());
        number(prmRow, 6, prmVendor.gallonsExtra(), prmStyles.number());
        number(prmRow, 7, prmVendor.gallonsMaxpro(), prmStyles.number());
        number(prmRow, 8, sum(prmVendor.gallonsMotor(), prmVendor.gallonsDiesel(),
                prmVendor.gallonsExtra(), prmVendor.gallonsMaxpro()), prmStyles.number());
        whole(prmRow, 9, additiveUnits(prmVendor.products()), prmStyles.integer());
        number(prmRow, 10, prmVendor.cash(), prmStyles.money());
        number(prmRow, 11, prmVendor.ga(), prmStyles.money());
        number(prmRow, 12, prmVendor.credibanco(), prmStyles.money());
        number(prmRow, 13, prmVendor.qr(), prmStyles.money());
        number(prmRow, 14, prmVendor.redemptions(), prmStyles.money());
        number(prmRow, 15, prmVendor.totalDelivered(), prmStyles.money());
        number(prmRow, 16, prmVendor.dominusTirilla(), prmStyles.money());
        number(prmRow, 17, prmVendor.realDiscrepancy(), objDifference);
        text(prmRow, 18, prmVendor.resultStatus(), resultStyle(prmVendor.resultStatus(), prmStyles));
    }

    private static void writeDayTotalRow(Row prmRow, DaySummary prmDay, Styles prmStyles) {
        text(prmRow, 0, "TOTAL DEL DÍA", prmStyles.totalText());
        number(prmRow, 4, prmDay.gallonsMotor(), prmStyles.totalNumber());
        number(prmRow, 5, prmDay.gallonsDiesel(), prmStyles.totalNumber());
        number(prmRow, 6, prmDay.gallonsExtra(), prmStyles.totalNumber());
        number(prmRow, 7, prmDay.gallonsMaxpro(), prmStyles.totalNumber());
        number(prmRow, 8, prmDay.totalFuelGallons(), prmStyles.totalNumber());
        whole(prmRow, 9, additiveUnits(prmDay.products()), prmStyles.totalInteger());
        number(prmRow, 10, prmDay.totalCash(), prmStyles.totalMoney());
        number(prmRow, 11, prmDay.totalGa(), prmStyles.totalMoney());
        number(prmRow, 12, prmDay.totalCredibanco(), prmStyles.totalMoney());
        number(prmRow, 13, prmDay.totalQr(), prmStyles.totalMoney());
        number(prmRow, 14, prmDay.totalRedemptions(), prmStyles.totalMoney());
        number(prmRow, 15, prmDay.totalDelivered(), prmStyles.totalMoney());
        number(prmRow, 16, prmDay.totalDominusTirilla(), prmStyles.totalMoney());
        number(prmRow, 17, prmDay.totalDiscrepancy(), prmStyles.totalMoney());
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
            text(objRow, 0, objProduct.sku(), prmStyles.text());
            text(objRow, 1, objProduct.productName(), prmStyles.text());
            text(objRow, 2, varGroup, prmStyles.text());
            number(objRow, 3, objProduct.unitPrice(), prmStyles.money());
            whole(objRow, 4, objProduct.unitsSold(), prmStyles.integer());
            number(objRow, 5, objProduct.totalAmount(), prmStyles.money());
            varTotalUnits += objProduct.unitsSold();
            objTotalAmount = objTotalAmount.add(zeroIfNull(objProduct.totalAmount()));
            objGroupTotals.merge(varGroup, zeroIfNull(objProduct.totalAmount()), BigDecimal::add);
        }
        Row objTotals = prmSheet.createRow(varRow++);
        text(objTotals, 1, "TOTALES", prmStyles.totalText());
        whole(objTotals, 4, varTotalUnits, prmStyles.totalInteger());
        number(objTotals, 5, objTotalAmount, prmStyles.totalMoney());
        varRow++;
        for (Map.Entry<String, BigDecimal> objEntry : objGroupTotals.entrySet()) {
            Row objSummary = prmSheet.createRow(varRow++);
            text(objSummary, 1, "VENTA " + objEntry.getKey(), prmStyles.totalText());
            number(objSummary, 5, objEntry.getValue(), prmStyles.totalMoney());
        }
        Row objBasket = prmSheet.createRow(varRow);
        text(objBasket, 1, "VENTA TOTAL CANASTA", prmStyles.totalText());
        number(objBasket, 5, objTotalAmount, prmStyles.totalMoney());
        ExcelWorkbookStyles.finish(prmSheet, PRODUCT_HEADERS.length);
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

    private static CellStyle resultStyle(String prmResult, Styles prmStyles) {
        if (prmResult == null || RESULT_BALANCED.equalsIgnoreCase(prmResult)) {
            return prmStyles.ok();
        }
        return RESULT_SHORTFALL.equalsIgnoreCase(prmResult) ? prmStyles.error() : prmStyles.warning();
    }

    private static CellStyle differenceStyle(String prmResult, Styles prmStyles) {
        if (prmResult == null || RESULT_BALANCED.equalsIgnoreCase(prmResult)) {
            return prmStyles.okMoney();
        }
        return RESULT_SHORTFALL.equalsIgnoreCase(prmResult) ? prmStyles.errorMoney() : prmStyles.warningMoney();
    }

    private static void text(Row prmRow, int prmColumn, String prmValue, CellStyle prmStyle) {
        Cell objCell = prmRow.createCell(prmColumn);
        objCell.setCellValue(prmValue == null ? "" : prmValue);
        objCell.setCellStyle(prmStyle);
    }

    private static void number(Row prmRow, int prmColumn, BigDecimal prmValue, CellStyle prmStyle) {
        Cell objCell = prmRow.createCell(prmColumn);
        if (prmValue == null) {
            objCell.setBlank();
        } else {
            objCell.setCellValue(prmValue.doubleValue());
        }
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
}
