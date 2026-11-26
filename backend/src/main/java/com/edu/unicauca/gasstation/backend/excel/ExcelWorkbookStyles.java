package com.edu.unicauca.gasstation.backend.excel;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Visual identity shared by every Excel file the application generates.
 *
 * <p>The palette is the one of the client's template {@code BELLAVISTA.xlsx}: navy headers with
 * white text, light green for totals and balanced rows, light amber for warnings, and light grey
 * for section labels. The only addition is the standard Excel red pair for discrepancies, because
 * the client's template has no negative case.</p>
 *
 * <p>It lives in its own module so both {@code sales} and {@code reports} can use it without one
 * depending on the other.</p>
 */
public final class ExcelWorkbookStyles {

    /** Currency of the station. Two decimals, as the cash balances are handled. */
    public static final String MONEY_FORMAT = "\"$\" #,##0.00";

    /** Gallons and other measured volumes. */
    public static final String GALLONS_FORMAT = "#,##0.00";

    /** Counts: units, planilla numbers, pumps. */
    public static final String WHOLE_FORMAT = "#,##0";

    private static final byte[] NAVY = {0x1B, 0x36, 0x5D};
    private static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] GREEN_FILL = {(byte) 0xE2, (byte) 0xEF, (byte) 0xDA};
    private static final byte[] GREEN_TEXT = {0x1E, 0x46, 0x20};
    private static final byte[] AMBER_FILL = {(byte) 0xFF, (byte) 0xF2, (byte) 0xCC};
    private static final byte[] AMBER_TEXT = {0x7F, 0x60, 0x00};
    private static final byte[] RED_FILL = {(byte) 0xFF, (byte) 0xC7, (byte) 0xCE};
    private static final byte[] RED_TEXT = {(byte) 0x9C, 0x00, 0x06};
    private static final byte[] GREY_FILL = {(byte) 0xED, (byte) 0xED, (byte) 0xED};
    private static final byte[] GREY_TEXT = {0x59, 0x59, 0x59};
    private static final byte[] BORDER = {(byte) 0xD9, (byte) 0xD9, (byte) 0xD9};

    private ExcelWorkbookStyles() {
    }

    /** Writes title (row 0), date (row 1) and headers (row 3). Returns the first data row index. */
    public static int writeHeading(Sheet sheet, String title, LocalDate date, String[] headers, Styles styles) {
        text(sheet.createRow(0), 0, title, styles.title());
        text(sheet.createRow(1), 0, "Fecha: " + date, styles.subtitle());
        Row header = sheet.createRow(3);
        header.setHeightInPoints(30);
        for (int column = 0; column < headers.length; column++) {
            text(header, column, headers[column], styles.header());
        }
        return 4;
    }

    /** Freezes the header row and widens every column to its content, capped so nothing is truncated. */
    public static void finish(Sheet sheet, int columns) {
        sheet.createFreezePane(0, 4);
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 640, 60 * 256));
        }
    }

    public static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    public static void number(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    public static void whole(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Cell styles of one workbook. A workbook has a hard limit of cell styles, so they are created
     * once per file and reused in every row.
     */
    public record Styles(
            CellStyle title,
            CellStyle subtitle,
            CellStyle header,
            CellStyle section,
            CellStyle text,
            CellStyle number,
            CellStyle integer,
            CellStyle money,
            CellStyle boldNumber,
            CellStyle boldMoney,
            CellStyle totalText,
            CellStyle totalNumber,
            CellStyle totalInteger,
            CellStyle totalMoney,
            CellStyle ok,
            CellStyle warning,
            CellStyle error,
            CellStyle okNumber,
            CellStyle warningNumber,
            CellStyle errorNumber,
            CellStyle okMoney,
            CellStyle warningMoney,
            CellStyle errorMoney) {

        public static Styles of(XSSFWorkbook workbook) {
            XSSFFont titleFont = font(workbook, true, 14, NAVY);
            XSSFFont subtitleFont = font(workbook, false, 10, GREY_TEXT);
            XSSFFont headerFont = font(workbook, true, 11, WHITE);
            XSSFFont sectionFont = font(workbook, true, 11, NAVY);
            XSSFFont boldFont = font(workbook, true, 11, GREEN_TEXT);
            XSSFFont strongFont = font(workbook, true, 11, null);
            XSSFFont plainFont = font(workbook, false, 11, null);
            XSSFFont okFont = font(workbook, true, 11, GREEN_TEXT);
            XSSFFont warningFont = font(workbook, true, 11, AMBER_TEXT);
            XSSFFont errorFont = font(workbook, true, 11, RED_TEXT);

            XSSFCellStyle header = bordered(workbook, headerFont, null, NAVY);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);

            XSSFCellStyle totalText = bordered(workbook, boldFont, null, GREEN_FILL);
            totalText.setBorderTop(BorderStyle.DOUBLE);
            totalText.setTopBorderColor(color(GREEN_TEXT));

            return new Styles(
                    plain(workbook, titleFont, null),
                    plain(workbook, subtitleFont, null),
                    header,
                    bordered(workbook, sectionFont, null, GREY_FILL),
                    bordered(workbook, plainFont, null, null),
                    bordered(workbook, plainFont, GALLONS_FORMAT, null),
                    bordered(workbook, plainFont, WHOLE_FORMAT, null),
                    bordered(workbook, plainFont, MONEY_FORMAT, null),
                    bordered(workbook, strongFont, GALLONS_FORMAT, null),
                    bordered(workbook, strongFont, MONEY_FORMAT, null),
                    totalText,
                    total(workbook, boldFont, GALLONS_FORMAT),
                    total(workbook, boldFont, WHOLE_FORMAT),
                    total(workbook, boldFont, MONEY_FORMAT),
                    bordered(workbook, okFont, null, GREEN_FILL),
                    bordered(workbook, warningFont, null, AMBER_FILL),
                    bordered(workbook, errorFont, null, RED_FILL),
                    bordered(workbook, okFont, GALLONS_FORMAT, GREEN_FILL),
                    bordered(workbook, warningFont, GALLONS_FORMAT, AMBER_FILL),
                    bordered(workbook, errorFont, GALLONS_FORMAT, RED_FILL),
                    bordered(workbook, okFont, MONEY_FORMAT, GREEN_FILL),
                    bordered(workbook, warningFont, MONEY_FORMAT, AMBER_FILL),
                    bordered(workbook, errorFont, MONEY_FORMAT, RED_FILL));
        }

        private static XSSFCellStyle total(XSSFWorkbook workbook, XSSFFont font, String format) {
            XSSFCellStyle style = bordered(workbook, font, format, GREEN_FILL);
            style.setBorderTop(BorderStyle.DOUBLE);
            style.setTopBorderColor(color(GREEN_TEXT));
            return style;
        }

        private static XSSFCellStyle plain(XSSFWorkbook workbook, XSSFFont font, String format) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            if (format != null) {
                style.setDataFormat(workbook.createDataFormat().getFormat(format));
            }
            return style;
        }

        private static XSSFCellStyle bordered(XSSFWorkbook workbook, XSSFFont font, String format, byte[] fill) {
            XSSFCellStyle style = plain(workbook, font, format);
            if (fill != null) {
                style.setFillForegroundColor(color(fill));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(color(BORDER));
            style.setBottomBorderColor(color(BORDER));
            style.setLeftBorderColor(color(BORDER));
            style.setRightBorderColor(color(BORDER));
            return style;
        }

        private static XSSFFont font(XSSFWorkbook workbook, boolean bold, int height, byte[] textColor) {
            XSSFFont font = workbook.createFont();
            font.setBold(bold);
            font.setFontHeightInPoints((short) height);
            if (textColor != null) {
                font.setColor(color(textColor));
            }
            return font;
        }
    }

    private static XSSFColor color(byte[] rgb) {
        return new XSSFColor(rgb, null);
    }
}
