package com.edu.unicauca.gasstation.backend.sales.infrastructure.export;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement.Row;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement.Values;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException;
import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/** Provisional Excel-like layout. Contains presentation rules only, never cash calculations. */
@Component
public class DominusCashPageRenderer {
    private static final float MARGIN = 24;
    private static final float LEFT_WIDTH = 462;
    private static final float RIGHT_X = MARGIN + LEFT_WIDTH + 14;
    private static final float RIGHT_WIDTH = PDRectangle.A4.getHeight() - MARGIN - RIGHT_X;
    private static final float FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 12;
    private static final float PADDING = 3;
    private static final Color PALE_YELLOW = new Color(255, 242, 204);
    private static final Values BLANK = new Values(null, null, null);

    /** Adds exactly one page, after measuring every cell to reject content that cannot fit. */
    public void appendPage(PDDocument document, DominusCashComplement data) throws IOException {
        // PDFBox objects are confined to one request, including the fonts used by the page.
        new Template().appendPage(document, data);
    }

    private static final class Template {
        private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        private void appendPage(PDDocument document, DominusCashComplement data) throws IOException {
            List<PlacedCell> cells = layout(data);
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.setLineWidth(0.6f);
                for (PlacedCell cell : cells) {
                    draw(stream, cell);
                }
            }
        }

        private List<PlacedCell> layout(DominusCashComplement data) throws IOException {
            List<PlacedCell> result = new ArrayList<>();
            float top = PDRectangle.A4.getWidth() - MARGIN;
            var header = data.header();
            top = place(result, MARGIN, top, List.of(
                    cell("DOMINUS", 244, Alignment.CENTER, true, null),
                    cell(header.reportDate().format(DateTimeFormatter.ofPattern("d/M/uuuu")), 108,
                            Alignment.CENTER, true, null),
                    cell("COD. " + header.stationCode() + "\n" + header.stationName(), 110,
                            Alignment.CENTER, true, null)));
            if (header.administrator() != null && !header.administrator().isBlank()) {
                top = place(result, MARGIN, top, List.of(
                        cell("Administrador: " + header.administrator(), LEFT_WIDTH, Alignment.LEFT, false, null)));
            }

            List<List<Cell>> left = new ArrayList<>();
            var lubricants = data.lubricantSales();
            left.add(section("Ventas de Lubricantes y Accesorios", LEFT_WIDTH));
            left.add(values("ACEITES", lubricants.oils()));
            left.add(values("ADIT. AGUAS Y LIQUIDOS", lubricants.additivesWaterLiquids()));
            left.add(values("MERCANCÍAS VARIAS", lubricants.miscellaneousGoods()));
            addRows(left, lubricants.additionalRows(), 0);
            left.add(total(lubricants.total()));

            var credit = data.creditSales();
            left.add(section("Ventas a Crédito", LEFT_WIDTH));
            left.add(values("Sotracauca", credit.sotracauca()));
            left.add(values("Puntos Colombia", credit.puntosColombia()));
            addRows(left, credit.additionalRows(), 2);
            left.add(total(credit.total()));

            left.add(section("Gastos del día", LEFT_WIDTH));
            addRows(left, data.expenses().rows(), 7);
            left.add(total(data.expenses().total()));

            var deposits = data.deposits();
            left.add(section("Consignaciones del día", LEFT_WIDTH));
            left.add(values("CREDIBANCO", deposits.credibanco()));
            left.add(values("BANCOLOMBIA PRIMAX", deposits.bancolombiaPrimax()));
            left.add(values("QR", deposits.qr()));
            addRows(left, deposits.additionalRows(), 1);
            left.add(total(deposits.total()));
            float leftTop = top;
            for (List<Cell> row : left) {
                leftTop = place(result, MARGIN, leftTop, row);
            }

            // Align the right table with the credit section, as in the supplied reference.
            float rightTop = top;
            for (int i = 0; i < 5 + lubricants.additionalRows().size(); i++) {
                rightTop -= height(left.get(i));
            }
            var balance = data.dailyBalance();
            rightTop = place(result, RIGHT_X, rightTop, section("SALDO DEL DIA", RIGHT_WIDTH));
            rightTop = place(result, RIGHT_X, rightTop, summary("VENTAS DEL DIA", balance.dailySales(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("GASTOS", balance.expenses(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("CREDIBANCOS", balance.credibancos(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("CREDITOS", balance.credits(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("QR", balance.qr(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("EFECTIVO DEL DIA", balance.dailyCash(), true, null));

            var cash = data.accumulatedCash();
            rightTop -= 36;
            rightTop = place(result, RIGHT_X, rightTop, section("CAJA ACUMULADA", RIGHT_WIDTH));
            rightTop = place(result, RIGHT_X, rightTop, summary("EFECTIVO ANTERIOR", cash.previousCash(), false, PALE_YELLOW));
            rightTop = place(result, RIGHT_X, rightTop, summary("EFECTIVO DEL DIA", cash.dailyCash(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("CONSIGNACION", cash.deposit(), false, null));
            rightTop = place(result, RIGHT_X, rightTop, summary("", null, false, null));
            place(result, RIGHT_X, rightTop, summary("SALDO ACUMULADO", cash.accumulatedBalance(), true, PALE_YELLOW));
            return result;
        }

        private static List<Cell> section(String title, float width) {
            return List.of(cell(title, width, Alignment.CENTER, true, null));
        }

        private static List<Cell> values(String concept, Values values) {
            boolean monetaryMiddle = values.intermediateAmount() != null;
            return List.of(cell(concept, 244, Alignment.LEFT, false, null),
                    cell(monetaryMiddle ? money(values.intermediateAmount()) : values.detail(), 108,
                            monetaryMiddle ? Alignment.RIGHT : Alignment.LEFT, false, null),
                    cell(money(values.amount()), 110, Alignment.RIGHT, false, null));
        }

        private static List<Cell> total(BigDecimal amount) {
            return List.of(cell("", 352, Alignment.LEFT, false, null),
                    cell(money(amount), 110, Alignment.RIGHT, true, Color.YELLOW));
        }

        private static List<Cell> summary(String label, BigDecimal value, boolean bold, Color highlight) {
            return List.of(cell(label, RIGHT_WIDTH - 110, Alignment.LEFT, bold,
                            bold ? highlight : null),
                    cell(money(value), 110, Alignment.RIGHT, bold, highlight));
        }

        private static void addRows(List<List<Cell>> target, List<Row> rows, int minimum) {
            for (Row row : rows) {
                target.add(values(row.concept(), row.values()));
            }
            for (int i = rows.size(); i < minimum; i++) {
                target.add(values("", BLANK));
            }
        }

        private static String money(BigDecimal value) {
            if (value == null) {
                return "";
            }
            // BigDecimal is formatted directly: no double conversion, rounding or recomputation.
            DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO")));
            format.setMinimumFractionDigits(Math.max(0, value.scale()));
            format.setMaximumFractionDigits(Math.max(0, value.scale()));
            return format.format(value);
        }

        private static Cell cell(String text, float width, Alignment alignment, boolean bold, Color background) {
            return new Cell(text == null ? "" : text, width, alignment, bold, background);
        }

        private float height(List<Cell> row) throws IOException {
            int lines = 1;
            for (Cell cell : row) {
                lines = Math.max(lines, wrap(cell).size());
            }
            return Math.max(18, lines * LINE_HEIGHT + 2 * PADDING);
        }

        private float place(List<PlacedCell> result, float x, float top, List<Cell> row) throws IOException {
            float height = height(row);
            if (top - height < MARGIN) {
                throw layoutError("Complement does not fit on one page; reduce additional rows or text length");
            }
            for (Cell cell : row) {
                result.add(new PlacedCell(cell, x, top, height, wrap(cell)));
                x += cell.width();
            }
            return top - height;
        }

        private List<String> wrap(Cell cell) throws IOException {
            PDFont font = cell.bold() ? bold : regular;
            float available = cell.width() - 2 * PADDING;
            List<String> lines = new ArrayList<>();
            try {
                for (String paragraph : cell.text().split("\\R", -1)) {
                    if (cell.alignment() == Alignment.RIGHT) {
                        if (width(font, paragraph) > available) {
                            throw layoutError("A monetary value exceeds its column width");
                        }
                        lines.add(paragraph);
                        continue;
                    }
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i < paragraph.length(); i++) {
                        char next = paragraph.charAt(i);
                        if (width(font, line.toString() + next) > available) {
                            int space = line.lastIndexOf(" ");
                            if (space > 0 && next != ' ') {
                                lines.add(line.substring(0, space));
                                String remainder = line.substring(space + 1);
                                line.setLength(0);
                                line.append(remainder);
                            } else {
                                lines.add(line.toString());
                                line.setLength(0);
                            }
                        }
                        line.append(next);
                    }
                    lines.add(line.toString());
                }
            } catch (IllegalArgumentException ex) {
                throw layoutError("Text contains characters unsupported by the provisional PDF font");
            }
            return lines;
        }

        private static float width(PDFont font, String text) throws IOException {
            return font.getStringWidth(text) / 1000 * FONT_SIZE;
        }

        private void draw(PDPageContentStream stream, PlacedCell placed) throws IOException {
            Cell cell = placed.cell();
            float bottom = placed.top() - placed.height();
            if (cell.background() != null) {
                stream.setNonStrokingColor(cell.background());
                stream.addRect(placed.x(), bottom, cell.width(), placed.height());
                stream.fill();
            }
            stream.setStrokingColor(Color.BLACK);
            stream.addRect(placed.x(), bottom, cell.width(), placed.height());
            stream.stroke();
            PDFont font = cell.bold() ? bold : regular;
            float baseline = placed.top() - PADDING - FONT_SIZE;
            stream.setNonStrokingColor(Color.BLACK);
            for (String line : placed.lines()) {
                float lineWidth = width(font, line);
                float textX = switch (cell.alignment()) {
                    case LEFT -> placed.x() + PADDING;
                    case RIGHT -> placed.x() + cell.width() - PADDING - lineWidth;
                    case CENTER -> placed.x() + (cell.width() - lineWidth) / 2;
                };
                stream.beginText();
                stream.setFont(font, FONT_SIZE);
                stream.newLineAtOffset(textX, baseline);
                stream.showText(line);
                stream.endText();
                baseline -= LINE_HEIGHT;
            }
        }

        private static DominusPdfException layoutError(String message) {
            return new DominusPdfException(DominusPdfException.Reason.LAYOUT_OVERFLOW, message);
        }

        private enum Alignment { LEFT, CENTER, RIGHT }
        private record Cell(String text, float width, Alignment alignment, boolean bold, Color background) { }
        private record PlacedCell(Cell cell, float x, float top, float height, List<String> lines) { }
    }
}
