package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Shared Apache POI helpers for the report workbooks.
 *
 * @author javiersolanop777
 */
final class ReportWorkbookSupport {

    private ReportWorkbookSupport() {
    }

    static int writeHeading(Sheet sheet, String title, LocalDate date, String[] headers, Styles styles) {
        text(sheet.createRow(0), 0, title, styles.title());
        text(sheet.createRow(1), 0, "Fecha: " + date, styles.text());
        Row header = sheet.createRow(3);
        for (int column = 0; column < headers.length; column++) {
            text(header, column, headers[column], styles.header());
        }
        return 4;
    }

    static void finish(Sheet sheet, int columns) {
        sheet.createFreezePane(0, 4);
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 60 * 256));
        }
    }

    static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    static void number(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    static void whole(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    record Styles(CellStyle title, CellStyle header, CellStyle text, CellStyle number, CellStyle integer,
                  CellStyle totalText, CellStyle totalNumber, CellStyle totalInteger) {

        static Styles of(Workbook workbook) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            Font big = workbook.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);
            short money = workbook.createDataFormat().getFormat("#,##0.00");
            short whole = workbook.createDataFormat().getFormat("#,##0");

            CellStyle header = style(workbook, bold, (short) 0);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            return new Styles(
                    style(workbook, big, (short) 0),
                    header,
                    style(workbook, null, (short) 0),
                    style(workbook, null, money),
                    style(workbook, null, whole),
                    style(workbook, bold, (short) 0),
                    style(workbook, bold, money),
                    style(workbook, bold, whole)
            );
        }

        private static CellStyle style(Workbook workbook, Font font, short format) {
            CellStyle style = workbook.createCellStyle();
            if (font != null) {
                style.setFont(font);
            }
            if (format != 0) {
                style.setDataFormat(format);
            }
            return style;
        }
    }
}
