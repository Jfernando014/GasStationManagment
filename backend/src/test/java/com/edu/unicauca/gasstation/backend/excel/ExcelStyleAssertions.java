package com.edu.unicauca.gasstation.backend.excel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 * Assertions over the visual identity of the generated Excel files, so a change of the palette or
 * of a number format is caught by the tests.
 */
public final class ExcelStyleAssertions {

    public static final byte[] NAVY = {0x1B, 0x36, 0x5D};
    public static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    public static final byte[] GREEN_FILL = {(byte) 0xE2, (byte) 0xEF, (byte) 0xDA};
    public static final byte[] GREEN_TEXT = {0x1E, 0x46, 0x20};
    public static final byte[] AMBER_FILL = {(byte) 0xFF, (byte) 0xF2, (byte) 0xCC};
    public static final byte[] RED_FILL = {(byte) 0xFF, (byte) 0xC7, (byte) 0xCE};
    public static final byte[] RED_TEXT = {(byte) 0x9C, (byte) 0x00, (byte) 0x06};
    public static final byte[] GREY_FILL = {(byte) 0xED, (byte) 0xED, (byte) 0xED};

    private ExcelStyleAssertions() {
    }

    /** Header cell: navy background, white bold text and thin borders. */
    public static void assertHeader(Cell prmCell) {
        assertFill(prmCell, NAVY);
        assertFontColor(prmCell, WHITE);
        assertTrue(fontOf(prmCell).getBold(), "The header font must be bold");
        assertThinBorders(prmCell);
    }

    /** Total cell: light green background with a double top border. */
    public static void assertTotal(Cell prmCell) {
        assertFill(prmCell, GREEN_FILL);
        assertFontColor(prmCell, GREEN_TEXT);
        assertTrue(fontOf(prmCell).getBold(), "The total font must be bold");
        assertEquals(BorderStyle.DOUBLE, prmCell.getCellStyle().getBorderTop(), "Totals need a double top border");
    }

    public static void assertFill(Cell prmCell, byte[] prmRgb) {
        XSSFColor varColor = ((XSSFCell) prmCell).getCellStyle().getFillForegroundColorColor();
        assertNotNull(varColor, "The cell has no fill color");
        assertArrayEquals(prmRgb, varColor.getRGB(), "Unexpected fill color");
    }

    public static void assertFontColor(Cell prmCell, byte[] prmRgb) {
        XSSFColor varColor = fontOf(prmCell).getXSSFColor();
        assertNotNull(varColor, "The cell has no font color");
        assertArrayEquals(prmRgb, varColor.getRGB(), "Unexpected font color");
    }

    public static void assertNumberFormat(Cell prmCell, String prmFormat) {
        assertEquals(prmFormat, prmCell.getCellStyle().getDataFormatString(), "Unexpected number format");
    }

    public static void assertThinBorders(Cell prmCell) {
        CellStyle varStyle = prmCell.getCellStyle();
        assertEquals(BorderStyle.THIN, varStyle.getBorderTop(), "The cell needs a top border");
        assertEquals(BorderStyle.THIN, varStyle.getBorderBottom(), "The cell needs a bottom border");
        assertEquals(BorderStyle.THIN, varStyle.getBorderLeft(), "The cell needs a left border");
        assertEquals(BorderStyle.THIN, varStyle.getBorderRight(), "The cell needs a right border");
    }

    private static XSSFFont fontOf(Cell prmCell) {
        return ((XSSFCell) prmCell).getCellStyle().getFont();
    }
}
