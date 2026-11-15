package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport.FuelVariationAggregate;
import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport.SellerAggregate;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.springframework.stereotype.Component;

/**
 * Builds the executive PowerPoint balance with Apache POI (XSLF).
 *
 * @author javiersolanop777
 */
@Component
public class BalancePresentationExporter {

    private static final Color DARK = new Color(11, 25, 44);
    private static final Color ORANGE = new Color(255, 107, 0);
    private static final Color BLUE = new Color(2, 132, 199);
    private static final Color LIGHT = new Color(244, 247, 250);
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);

    private static final double SLIDE_WIDTH = 960;
    private static final double SLIDE_HEIGHT = 540;

    public byte[] export(BalanceReport report) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension((int) SLIDE_WIDTH, (int) SLIDE_HEIGHT));
            addCover(ppt, report);
            addDailyEvolution(ppt, report);
            addSalesAndSellers(ppt, report);
            addInventory(ppt, report);
            addCashAndConclusions(ppt, report);
            ppt.write(output);
            return output.toByteArray();
        }
    }

    private static void addCover(XMLSlideShow ppt, BalanceReport report) {
        XSLFSlide slide = newSlide(ppt, "BALANCE OPERATIVO", "Período " + report.from() + " a " + report.to(), 1);
        addText(slide, "Resumen ejecutivo", 40, 115, 880, 35, 24, true, DARK);
        addKpi(slide, "Días con datos", String.valueOf(report.days().size()), 40, 165, 210);
        addKpi(slide, "Días en sistema", String.valueOf(report.liveDays()), 265, 165, 210);
        addKpi(slide, "Días migrados", String.valueOf(report.migratedDays()), 490, 165, 210);
        addKpi(slide, "Alertas de inventario", String.valueOf(alertCount(report)), 715, 165, 205);
        addKpi(slide, "Galones vendidos", number(report.totalGallons()), 40, 285, 210);
        addKpi(slide, "Total entregado", money(report.totalDelivered()), 265, 285, 210);
        addKpi(slide, "Canastilla Dominus", money(report.totalBasket()), 490, 285, 210);
        addKpi(slide, "Descuadre total", money(report.totalDiscrepancy()), 715, 285, 205);
        addText(slide, "Efectivo del período: " + money(report.totalCash()), 40, 415, 880, 30, 18, true, DARK);
        addText(slide, "El balance usa los cierres de vendedores, la variación de inventario y la caja/Dominus. "
                + "Los días sin información se omiten y se informa su ausencia.", 40, 455, 880, 40, 12, false, GRAY);
    }

    private static void addDailyEvolution(XMLSlideShow ppt, BalanceReport report) {
        XSLFSlide slide = newSlide(ppt, "EVOLUCIÓN DIARIA", "Galones y total entregado por día", 2);
        List<String> labels = report.days().stream().map(day -> day.date().toString().substring(8)).toList();
        List<BigDecimal> gallons = report.days().stream().map(ReportDay::totalGallons).toList();
        addBars(slide, labels, gallons, 55, 130, 850, 280, BLUE, true);
        addText(slide, "Galones por día", 55, 420, 400, 25, 14, true, DARK);
        addText(slide, "Promedio diario: " + number(average(gallons)) + " galones", 55, 450, 400, 25, 12, false, GRAY);
        addText(slide, "Días graficados: " + report.days().size(), 555, 450, 350, 25, 12, false, GRAY);
    }

    private static void addSalesAndSellers(XMLSlideShow ppt, BalanceReport report) {
        XSLFSlide slide = newSlide(ppt, "VENTAS Y DESEMPEÑO", "Mezcla de combustibles y ranking de vendedores", 3);
        Map<String, BigDecimal> fuel = fuelSales(report);
        addBars(slide, new ArrayList<>(fuel.keySet()), new ArrayList<>(fuel.values()),
                55, 130, 410, 260, ORANGE, true);
        addText(slide, "Galones por combustible", 55, 400, 410, 25, 14, true, DARK);

        List<SellerAggregate> sellers = report.sellers().stream().limit(8).toList();
        List<String[]> rows = sellers.stream()
                .map(seller -> new String[]{seller.vendorName(), number(seller.gallons()),
                        money(seller.totalDelivered()), money(seller.discrepancy())})
                .toList();
        addTable(slide, new String[]{"Vendedor", "Galones", "Entregado", "Descuadre"},
                rows, 500, 125, 405, new double[]{150, 75, 95, 85});
        addText(slide, "Ranking por total entregado", 500, 400, 405, 25, 14, true, DARK);
        addText(slide, "Se muestran hasta 8 vendedores de " + report.sellers().size() + ".", 500, 430, 405, 25, 12, false, GRAY);
    }

    private static void addInventory(XMLSlideShow ppt, BalanceReport report) {
        XSLFSlide slide = newSlide(ppt, "VARIACIÓN DE INVENTARIO", "Teórico contra medido por combustible", 4);
        List<String[]> rows = report.fuelVariations().stream()
                .map(variation -> new String[]{variation.fuelType(), number(variation.theoreticalGallons()),
                        number(variation.measuredGallons()), number(variation.varianceGallons()),
                        number(variation.toleranceGallons()), variation.outOfTolerance() ? "FUERA" : "OK"})
                .toList();
        addTable(slide, new String[]{"Combustible", "Teórico", "Medido", "Variación", "Tolerancia", "Estado"},
                rows, 55, 125, 850, new double[]{150, 130, 130, 130, 130, 100});
        addText(slide, "Alertas de inventario del período: " + alertCount(report), 55, 400, 850, 25, 14, true, DARK);
        addText(slide, "Una variación fuera de tolerancia se marca como FUERA. Los días sin medición no inventan valores.",
                55, 430, 850, 25, 12, false, GRAY);
    }

    private static void addCashAndConclusions(XMLSlideShow ppt, BalanceReport report) {
        XSLFSlide slide = newSlide(ppt, "CAJA, DOMINUS Y CONCLUSIONES", "Cierre del período y disponibilidad de fuentes", 5);
        ReportDay.CashSummary cash = report.lastCash();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Planilla No.", cash == null || cash.planilla() == null ? "No disponible" : cash.planilla().toString()});
        rows.add(new String[]{"Saldo en caja", cash == null ? "No disponible" : money(cash.cashBalance())});
        rows.add(new String[]{"Efectivo del día", cash == null ? "No disponible" : money(cash.dailyCash())});
        rows.add(new String[]{"Saldo planilla", cash == null ? "No disponible" : money(cash.planillaCashBalance())});
        rows.add(new String[]{"Saldo Dominus", cash == null ? "No disponible" : money(cash.dominusAccumulatedBalance())});
        rows.add(new String[]{"Diferencia", cash == null ? "No disponible" : money(cash.difference())});
        addTable(slide, new String[]{"Concepto", "Valor"}, rows, 55, 125, 470, new double[]{240, 230});

        addText(slide, "Conclusiones", 560, 125, 345, 25, 16, true, DARK);
        addText(slide, "• " + report.days().size() + " días con información en el período.", 560, 165, 345, 25, 12, false, DARK);
        addText(slide, "• " + report.liveDays() + " días desde el sistema y " + report.migratedDays() + " migrados.", 560, 195, 345, 40, 12, false, DARK);
        addText(slide, "• Descuadre acumulado: " + money(report.totalDiscrepancy()) + ".", 560, 240, 345, 25, 12, false, DARK);
        addText(slide, "• Alertas de inventario: " + alertCount(report) + ".", 560, 270, 345, 25, 12, false, DARK);
        addText(slide, "Fuentes: SellerCloseQuery, InventoryClosingQuery, CashBalanceQuery y MigratedHistoryQuery. "
                + "Los valores no disponibles se muestran como tales; no se inventan cifras.", 560, 315, 345, 100, 11, false, GRAY);
    }

    private static XSLFSlide newSlide(XMLSlideShow ppt, String title, String subtitle, int number) {
        XSLFSlide slide = ppt.createSlide();
        slide.getBackground().setFillColor(LIGHT);
        XSLFAutoShape header = slide.createAutoShape();
        header.setShapeType(ShapeType.RECT);
        header.setAnchor(new Rectangle2D.Double(0, 0, SLIDE_WIDTH, 95));
        header.setFillColor(DARK);
        header.setLineColor(DARK);
        addText(slide, "EDS BELLAVISTA · BALANCE OPERATIVO", 40, 15, 700, 20, 11, true, ORANGE);
        addText(slide, title, 40, 40, 700, 35, 24, true, WHITE);
        addText(slide, subtitle, 40, 105, 880, 20, 11, false, GRAY);
        addText(slide, String.valueOf(number), 900, 505, 30, 20, 10, false, GRAY);
        return slide;
    }

    private static void addKpi(XSLFSlide slide, String label, String value, double x, double y, double width) {
        XSLFAutoShape card = slide.createAutoShape();
        card.setShapeType(ShapeType.ROUND_RECT);
        card.setAnchor(new Rectangle2D.Double(x, y, width, 95));
        card.setFillColor(WHITE);
        card.setLineColor(BORDER);
        addText(slide, label, x + 15, y + 12, width - 30, 20, 11, false, GRAY);
        addText(slide, value, x + 15, y + 38, width - 30, 35, 19, true, DARK);
    }

    private static void addTable(XSLFSlide slide, String[] headers, List<String[]> rows,
                                 double x, double y, double width, double[] columnWidths) {
        XSLFTable table = slide.createTable();
        table.setAnchor(new Rectangle2D.Double(x, y, width, 25 + rows.size() * 24));
        XSLFTableRow header = table.addRow();
        for (int index = 0; index < headers.length; index++) {
            XSLFTableCell cell = header.addCell();
            cell(cell, headers[index], true, DARK, WHITE);
            table.setColumnWidth(index, columnWidths[index]);
        }
        for (String[] row : rows) {
            XSLFTableRow tableRow = table.addRow();
            for (int index = 0; index < headers.length; index++) {
                XSLFTableCell cell = tableRow.addCell();
                cell(cell, index < row.length ? row[index] : "", false, WHITE, DARK);
            }
        }
    }

    private static void cell(XSLFTableCell cell, String value, boolean bold, Color background, Color foreground) {
        cell.setText(value == null ? "" : value);
        cell.setFillColor(background);
        for (XSLFTextParagraph paragraph : cell.getTextParagraphs()) {
            paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                run.setFontSize(10.0);
                run.setBold(bold);
                run.setFontColor(foreground);
            }
        }
    }

    private static void addBars(XSLFSlide slide, List<String> labels, List<BigDecimal> values,
                                double x, double y, double width, double height, Color color, boolean showValues) {
        if (labels.isEmpty()) {
            addText(slide, "Sin información para graficar.", x, y + height / 2, width, 25, 12, false, GRAY);
            return;
        }
        double max = values.stream().filter(value -> value != null)
                .mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double slot = width / labels.size();
        double barWidth = Math.max(slot * 0.65, 3);
        for (int index = 0; index < labels.size(); index++) {
            BigDecimal value = values.get(index);
            double numeric = value == null ? 0 : Math.max(value.doubleValue(), 0);
            double barHeight = max == 0 ? 0 : numeric / max * height;
            XSLFAutoShape bar = slide.createAutoShape();
            bar.setShapeType(ShapeType.RECT);
            bar.setAnchor(new Rectangle2D.Double(x + index * slot + (slot - barWidth) / 2,
                    y + height - barHeight, barWidth, barHeight));
            bar.setFillColor(color);
            bar.setLineColor(color);
            if (showValues && labels.size() <= 8) {
                addText(slide, number(value), x + index * slot, y + height - barHeight - 22,
                        slot, 20, 9, true, DARK);
            }
            if (labels.size() <= 15 || index % 5 == 0) {
                addText(slide, labels.get(index), x + index * slot, y + height + 8,
                        slot, 18, labels.size() <= 8 ? 10 : 8, false, GRAY);
            }
        }
    }

    private static void addText(XSLFSlide slide, String text, double x, double y, double width, double height,
                                double fontSize, boolean bold, Color color) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        box.setWordWrap(true);
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text == null ? "" : text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontColor(color);
    }

    private static int alertCount(BalanceReport report) {
        return report.days().stream().mapToInt(ReportDay::alertCount).sum();
    }

    private static Map<String, BigDecimal> fuelSales(BalanceReport report) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("MOTOR", BigDecimal.ZERO);
        result.put("DIESEL", BigDecimal.ZERO);
        result.put("EXTRA", BigDecimal.ZERO);
        result.put("MAXPRO", BigDecimal.ZERO);
        for (ReportDay day : report.days()) {
            for (ReportDay.FuelSale sale : day.fuelSales()) {
                result.merge(sale.fuelType(), sale.gallons() == null ? BigDecimal.ZERO : sale.gallons(), BigDecimal::add);
            }
        }
        return result;
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = values.stream().filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static String number(BigDecimal value) {
        if (value == null) {
            return "No disponible";
        }
        return formatter("#,##0.00").format(value);
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "No disponible";
        }
        return "$ " + formatter("#,##0.00").format(value);
    }

    private static DecimalFormat formatter(String pattern) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat(pattern, symbols);
    }
}
