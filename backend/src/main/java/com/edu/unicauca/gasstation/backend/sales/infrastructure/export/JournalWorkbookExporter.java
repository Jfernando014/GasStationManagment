package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
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

@Component
public class JournalWorkbookExporter {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(JournalSnapshot journal) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            createSummarySheet(workbook, journal, headerStyle);
            createStepsSheet(workbook, journal, headerStyle);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void createSummarySheet(Workbook workbook, JournalSnapshot journal, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Jornada");
        Row header = sheet.createRow(0);
        writeCell(header, 0, "Campo", headerStyle);
        writeCell(header, 1, "Valor", headerStyle);

        writeRow(sheet, 1, "ID de jornada", journal.id().toString());
        writeRow(sheet, 2, "Fecha", journal.date().toString());
        writeRow(sheet, 3, "Consecutivo", journal.consecutive().toString());
        writeRow(sheet, 4, "Estado", journal.status().name());
        writeRow(sheet, 5, "Creada el", format(journal.createdAt()));
        finishSheet(sheet, 2, 1);
    }

    private static void createStepsSheet(Workbook workbook, JournalSnapshot journal, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Pasos");
        Row header = sheet.createRow(0);
        writeCell(header, 0, "N.º paso", headerStyle);
        writeCell(header, 1, "Nombre del paso", headerStyle);
        writeCell(header, 2, "Estado", headerStyle);
        writeCell(header, 3, "Completado el", headerStyle);

        int rowNumber = 1;
        for (JournalSnapshot.JournalStepSnapshot step : journal.steps()) {
            Row row = sheet.createRow(rowNumber++);
            row.createCell(0).setCellValue(step.stepNumber());
            row.createCell(1).setCellValue(step.name());
            row.createCell(2).setCellValue(step.status().name());
            row.createCell(3).setCellValue(format(step.completedAt()));
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rowNumber - 1), 0, 3));
        finishSheet(sheet, 4, 1);
    }

    private static void writeRow(Sheet sheet, int rowNumber, String label, String value) {
        Row row = sheet.createRow(rowNumber);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    private static void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void finishSheet(Sheet sheet, int columns, int frozenRows) {
        sheet.createFreezePane(0, frozenRows);
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 60 * 256));
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static String format(java.time.LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMAT);
    }
}