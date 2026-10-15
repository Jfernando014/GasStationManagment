package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.sales.DominusPdfTestData;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement.Row;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement.Values;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException.Reason;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DominusCashPageRenderer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs without Spring, a datasource, repositories or migrations. */
class DominusPdfServiceTest {
    private static ValidatorFactory factory;
    private static DominusPdfService service;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        service = new DominusPdfService(new DominusCashPageRenderer(), factory.getValidator());
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void preservesOriginalPagesInOrderAndAppendsExactlyOnePage() throws Exception {
        byte[] original = DominusPdfTestData.originalPdf();
        byte[] unchanged = original.clone();
        byte[] result = service.appendCashComplement(original, DominusPdfTestData.sample());
        try (PDDocument before = Loader.loadPDF(original); PDDocument after = Loader.loadPDF(result)) {
            assertThat(after.getNumberOfPages()).isEqualTo(before.getNumberOfPages() + 1);
            PDFRenderer beforeRenderer = new PDFRenderer(before);
            PDFRenderer afterRenderer = new PDFRenderer(after);
            for (int i = 0; i < before.getNumberOfPages(); i++) {
                PDPage expected = before.getPage(i);
                PDPage actual = after.getPage(i);
                assertThat(actual.getMediaBox().getCOSArray().toFloatArray()).isEqualTo(expected.getMediaBox().getCOSArray().toFloatArray());
                assertThat(actual.getCropBox().getCOSArray().toFloatArray()).isEqualTo(expected.getCropBox().getCOSArray().toFloatArray());
                assertThat(actual.getRotation()).isEqualTo(expected.getRotation());
                assertThat(actual.getContents().readAllBytes()).isEqualTo(expected.getContents().readAllBytes());
                assertThat(text(after, i + 1)).isEqualTo(text(before, i + 1));
                BufferedImage image = beforeRenderer.renderImageWithDPI(i, 72);
                BufferedImage rendered = afterRenderer.renderImageWithDPI(i, 72);
                assertThat(rendered.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                        .isEqualTo(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
            }
            assertThat(text(after, 3)).contains("DOMINUS", "1/8/2026", "COD. 761", "BELLAVISTA",
                    "Ventas de Lubricantes y Accesorios", "ACEITES", "ADIT. AGUAS Y LIQUIDOS", "MERCANCÍAS VARIAS",
                    "Ventas a Crédito", "Sotracauca", "Puntos Colombia", "Gastos del día", "Consignaciones del día",
                    "CREDIBANCO", "BANCOLOMBIA PRIMAX", "SALDO DEL DIA", "CAJA ACUMULADA",
                    "VENTAS DEL DIA", "CREDIBANCOS", "CREDITOS", "EFECTIVO ANTERIOR", "CONSIGNACION", "SALDO ACUMULADO");
        }
        assertThat(original).isEqualTo(unchanged);
    }

    @Test
    void preservesPreciseZeroNegativeIntermediateAndIndependentTotals() throws Exception {
        var data = DominusPdfTestData.mapper().readTree(DominusPdfTestData.sampleJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("lubricantSales").path("oils"))
                .put("intermediateAmount", new BigDecimal("-123.45"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("lubricantSales").path("oils"))
                .put("amount", new BigDecimal("999999999999.99"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("creditSales").path("sotracauca"))
                .put("amount", new BigDecimal("0.00"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("dailyBalance"))
                .put("dailyCash", new BigDecimal("-17.01"));
        DominusCashComplement request = DominusPdfTestData.mapper().treeToValue(data, DominusCashComplement.class);
        assertThat(request.lubricantSales().oils().amount()).isEqualByComparingTo("999999999999.99");
        byte[] result = service.appendCashComplement(DominusPdfTestData.originalPdf(), request);
        try (PDDocument document = Loader.loadPDF(result)) {
            assertThat(text(document, 3)).contains("999.999.999.999,99", "-123,45", "0,00", "-17,01",
                    "458.000", "46.055", "12.515.926", "24.629.469");
            assertTextInsideCells(document.getPage(2));
        }
    }

    @Test
    void preservesBlankRowsAndMiddleColumnsAndWritesReviewableDemo() throws Exception {
        byte[] original = DominusPdfTestData.originalPdf();
        byte[] result = service.appendCashComplement(original, DominusPdfTestData.sample());
        Path directory = Path.of("target", "dominus-demo");
        Files.createDirectories(directory);
        Files.write(directory.resolve("original.pdf"), original);
        Files.write(directory.resolve("dominus-complement.pdf"), result);
        try (PDDocument document = Loader.loadPDF(result)) {
            List<Rectangle2D.Float> cells = cells(document.getPage(2));
            // Three columns: 3 lubricants + 4 credits + 7 expenses + 4 deposits.
            assertThat(cells.stream().filter(cell -> cell.width == 108 && cell.height == 18).count()).isEqualTo(18);
            // The accumulated cash table retains its blank row and its four value rows.
            assertThat(cells.stream().filter(cell -> Math.abs(cell.x - 500) < 0.1 && cell.width < 220).count())
                    .isEqualTo(11);
            assertTextInsideCells(document.getPage(2));
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(2, 144), "png",
                    directory.resolve("complement-preview.png").toFile());
        }
    }

    @Test
    void wrapsLongTextAndPreservesExtraRowsWithoutClipping() throws Exception {
        var data = DominusPdfTestData.sample();
        var request = new DominusCashComplement(
                new DominusCashComplement.Header(data.header().reportDate(), "761", "BELLAVISTA",
                        "Administrador de estación Bellavista"),
                new DominusCashComplement.LubricantSales(data.lubricantSales().oils(),
                        data.lubricantSales().additivesWaterLiquids(), data.lubricantSales().miscellaneousGoods(),
                        List.of(new Row("Concepto adicional", new Values(null, BigDecimal.ZERO, new BigDecimal("-5.50")))),
                        data.lubricantSales().total()),
                data.creditSales(),
                new DominusCashComplement.Expenses(List.of(
                        new Row("Detalle de gasto informado por administrador de estación", new Values("Soporte del gasto", null, BigDecimal.ZERO)),
                        new Row("", new Values(null, null, null))), data.expenses().total()),
                data.deposits(), data.dailyBalance(), data.accumulatedCash());
        byte[] result = service.appendCashComplement(DominusPdfTestData.originalPdf(), request);
        try (PDDocument document = Loader.loadPDF(result)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
            assertThat(text(document, 3)).contains("Concepto adicional", "Soporte del gasto", "-5,50", "Administrador:");
            assertTextInsideCells(document.getPage(2));
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(2, 144), "png",
                    Path.of("target", "dominus-stress-preview.png").toFile());
        }
    }

    @Test
    void rejectsExcessRowsRatherThanDroppingOrClippingThem() throws Exception {
        var data = DominusPdfTestData.sample();
        var request = new DominusCashComplement(data.header(), data.lubricantSales(), data.creditSales(),
                new DominusCashComplement.Expenses(java.util.Collections.nCopies(20,
                        new Row("Gasto informado", new Values("Detalle completo", null, BigDecimal.ZERO))), BigDecimal.ZERO),
                data.deposits(), data.dailyBalance(), data.accumulatedCash());
        assertReason(Reason.LAYOUT_OVERFLOW, DominusPdfTestData.originalPdf(), request);
    }

    @Test
    void rejectsEmptyCorruptTruncatedEncryptedAndPagelessPdfs() throws Exception {
        var data = DominusPdfTestData.sample();
        assertReason(Reason.EMPTY, null, data);
        assertReason(Reason.EMPTY, new byte[0], data);
        assertReason(Reason.INVALID_PDF, "not a pdf".getBytes(StandardCharsets.UTF_8), data);
        assertReason(Reason.INVALID_PDF, "%PDF-1.7\ncorrupt".getBytes(StandardCharsets.UTF_8), data);
        byte[] original = DominusPdfTestData.originalPdf();
        assertReason(Reason.INVALID_PDF, Arrays.copyOf(original, original.length / 2), data);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.save(output);
            assertReason(Reason.INVALID_PDF, output.toByteArray(), data);
        }
        try (PDDocument document = Loader.loadPDF(original); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.protect(new StandardProtectionPolicy("owner", "", new AccessPermission()));
            document.save(output);
            assertReason(Reason.INVALID_PDF, output.toByteArray(), data);
        }
    }

    @Test
    void validatesDirectServiceCallsAndFileSize() throws Exception {
        assertReason(Reason.TOO_LARGE, new byte[(int) DominusPdfService.MAX_FILE_SIZE_BYTES + 1], DominusPdfTestData.sample());
        assertReason(Reason.INVALID_DATA, DominusPdfTestData.originalPdf(), null);
        var data = DominusPdfTestData.sample();
        assertReason(Reason.INVALID_DATA, DominusPdfTestData.originalPdf(), new DominusCashComplement(
                null, data.lubricantSales(), data.creditSales(), data.expenses(), data.deposits(), data.dailyBalance(), data.accumulatedCash()));
        assertThat(factory.getValidator().validate(new Values("detail", BigDecimal.ONE, null))).isNotEmpty();
        assertThat(factory.getValidator().validate(new Values(null, null, new BigDecimal("1.001")))).isNotEmpty();
        assertThat(factory.getValidator().validate(new Values(null, null, new BigDecimal("1000000000000")))).isNotEmpty();
        assertThat(DominusPdfTestData.mapper().valueToTree(new Values(null, null, BigDecimal.ZERO))
                .has("middleColumnUnambiguous")).isFalse();
    }

    @Test
    void parallelRequestsKeepPdfObjectsAndValuesSeparate() throws Exception {
        byte[] input = DominusPdfTestData.originalPdf();
        try (var executor = Executors.newFixedThreadPool(3)) {
            List<Callable<byte[]>> tasks = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                String sample = new String(DominusPdfTestData.sampleJson(), StandardCharsets.UTF_8)
                        .replace("192000", i + ".01");
                var request = DominusPdfTestData.mapper().readValue(sample, DominusCashComplement.class);
                tasks.add(() -> service.appendCashComplement(input, request));
            }
            var results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                try (PDDocument document = Loader.loadPDF(results.get(i).get())) {
                    assertThat(document.getNumberOfPages()).isEqualTo(3);
                    assertThat(text(document, 3)).contains((i + 1) + ",01", "458.000");
                }
            }
        }
    }

    @Test
    void everyMonetaryCellUsesItsOwnJsonValueWithoutSampleDefaults() throws Exception {
        ObjectNode json = (ObjectNode) DominusPdfTestData.mapper().readTree(DominusPdfTestData.sampleJson());
        ((ObjectNode) json.path("header")).put("reportDate", "2027-02-03")
                .put("stationCode", "999").put("stationName", "TEST STATION");
        ((com.fasterxml.jackson.databind.node.ArrayNode) json.path("lubricantSales").path("additionalRows"))
                .addObject().put("concept", "Additional sale").putObject("values");
        ((com.fasterxml.jackson.databind.node.ArrayNode) json.path("expenses").path("rows"))
                .addObject().put("concept", "Expense").putObject("values");

        List<ObjectNode> monetaryObjects = valueObjects(json);
        for (ObjectNode values : monetaryObjects) {
            values.putNull("intermediateAmount").putNull("amount");
        }
        for (String section : List.of("lubricantSales", "creditSales", "expenses", "deposits")) {
            monetaryObjects.add((ObjectNode) json.path(section));
        }
        monetaryObjects.add((ObjectNode) json.path("dailyBalance"));
        monetaryObjects.add((ObjectNode) json.path("accumulatedCash"));

        List<String> expectedAmounts = new ArrayList<>();
        int sequence = 0;
        for (ObjectNode object : monetaryObjects) {
            List<String> fields = new ArrayList<>();
            object.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                if (object.get(field).isValueNode()
                        && List.of("amount", "intermediateAmount", "total", "dailySales", "expenses", "credibancos",
                        "credits", "qr", "dailyCash", "previousCash", "deposit", "accumulatedBalance").contains(field)) {
                    sequence++;
                    object.put(field, new BigDecimal((10000 + sequence) + ".11"));
                    expectedAmounts.add("10.%03d,11".formatted(sequence));
                }
            }
        }
        assertThat(expectedAmounts).hasSize(40);
        try (PDDocument document = Loader.loadPDF(renderJson(json))) {
            String page = text(document, 3);
            assertThat(page).contains(expectedAmounts.toArray(String[]::new))
                    .contains("3/2/2027", "COD. 999", "TEST STATION")
                    .doesNotContain("192.000", "458.000", "46.055", "5.035.696", "24.629.469");
            assertTextInsideCells(document.getPage(2));
        }
    }

    @Test
    void omittedAndNullOptionalCellsStayBlankWhileZeroRemainsVisibleInTheSameGrid() throws Exception {
        ObjectNode omitted = (ObjectNode) DominusPdfTestData.mapper().readTree(DominusPdfTestData.sampleJson());
        ((ObjectNode) omitted.path("header")).remove("administrator");
        ((com.fasterxml.jackson.databind.node.ArrayNode) omitted.path("expenses").path("rows"))
                .addObject().putNull("concept").putObject("values");
        valueObjects(omitted).forEach(ObjectNode::removeAll);

        ObjectNode explicitNull = omitted.deepCopy();
        ((ObjectNode) explicitNull.path("header")).putNull("administrator");
        valueObjects(explicitNull).forEach(values -> values.putNull("detail")
                .putNull("intermediateAmount").putNull("amount"));
        ObjectNode zero = explicitNull.deepCopy();
        List<ObjectNode> zeroCells = valueObjects(zero);
        zeroCells.forEach(values -> values.put("intermediateAmount", BigDecimal.ZERO).put("amount", BigDecimal.ZERO));

        try (PDDocument absentPdf = Loader.loadPDF(renderJson(omitted));
                PDDocument nullPdf = Loader.loadPDF(renderJson(explicitNull));
                PDDocument zeroPdf = Loader.loadPDF(renderJson(zero))) {
            PDPage absentPage = absentPdf.getPage(2);
            PDPage nullPage = nullPdf.getPage(2);
            PDPage zeroPage = zeroPdf.getPage(2);
            assertThat(nullPage.getContents().readAllBytes()).isEqualTo(absentPage.getContents().readAllBytes());
            assertThat(cells(nullPage)).isEqualTo(cells(absentPage)).isEqualTo(cells(zeroPage));
            assertThat(cells(absentPage).stream().filter(cell -> cell.width == 108 && cell.height == 18).count())
                    .isEqualTo(18);
            String blankStream = new String(absentPage.getContents().readAllBytes(), StandardCharsets.US_ASCII);
            String zeroStream = new String(zeroPage.getContents().readAllBytes(), StandardCharsets.US_ASCII);
            Pattern printedZero = Pattern.compile("\\(0\\) Tj");
            long existingRequiredZeros = printedZero.matcher(blankStream).results().count();
            assertThat(printedZero.matcher(zeroStream).results().count())
                    .isEqualTo(existingRequiredZeros + zeroCells.size() * 2L);
            assertTextInsideCells(zeroPage);
        }
    }

    private static List<ObjectNode> valueObjects(ObjectNode json) {
        List<ObjectNode> result = new ArrayList<>();
        for (String field : List.of("oils", "additivesWaterLiquids", "miscellaneousGoods")) {
            result.add((ObjectNode) json.path("lubricantSales").path(field));
        }
        for (String field : List.of("sotracauca", "puntosColombia")) {
            result.add((ObjectNode) json.path("creditSales").path(field));
        }
        for (String field : List.of("credibanco", "bancolombiaPrimax", "qr")) {
            result.add((ObjectNode) json.path("deposits").path(field));
        }
        json.findValues("values").forEach(values -> result.add((ObjectNode) values));
        return result;
    }

    private static byte[] renderJson(ObjectNode json) throws IOException {
        var mapper = DominusPdfTestData.mapper();
        var request = mapper.readValue(mapper.writeValueAsBytes(json), DominusCashComplement.class);
        return service.appendCashComplement(DominusPdfTestData.originalPdf(), request);
    }

    private static void assertReason(Reason reason, byte[] input, DominusCashComplement data) {
        assertThat(assertThrows(DominusPdfException.class, () -> service.appendCashComplement(input, data)).getReason())
                .isEqualTo(reason);
    }

    private static String text(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document);
    }

    private static List<Rectangle2D.Float> cells(PDPage page) throws IOException {
        // Inspect generated vector rectangles; deduplicate background-fill and border rectangles.
        String stream = new String(page.getContents().readAllBytes(), StandardCharsets.US_ASCII);
        var matcher = Pattern.compile("([\\d.]+) ([\\d.]+) ([\\d.]+) ([\\d.]+) re").matcher(stream);
        List<Rectangle2D.Float> cells = new ArrayList<>();
        while (matcher.find()) {
            Rectangle2D.Float cell = new Rectangle2D.Float(Float.parseFloat(matcher.group(1)),
                    Float.parseFloat(matcher.group(2)), Float.parseFloat(matcher.group(3)), Float.parseFloat(matcher.group(4)));
            if (!cells.contains(cell)) {
                cells.add(cell);
            }
        }
        return cells;
    }

    private static void assertTextInsideCells(PDPage page) throws IOException {
        List<Rectangle2D.Float> cells = cells(page);
        for (Rectangle2D.Float cell : cells) {
            assertThat(cell.x).isGreaterThanOrEqualTo(24);
            assertThat(cell.y).isGreaterThanOrEqualTo(24);
            assertThat(cell.getMaxX()).isLessThanOrEqualTo(page.getMediaBox().getWidth() - 23.9);
            assertThat(cell.getMaxY()).isLessThanOrEqualTo(page.getMediaBox().getHeight() - 23.9);
        }
        // Check every glyph against its cell and every pair of glyphs for intersections.
        List<Rectangle2D.Float> glyphs = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition position) {
                if (position.getUnicode().isBlank()) {
                    return;
                }
                float baseline = page.getMediaBox().getHeight() - position.getYDirAdj();
                Rectangle2D.Float glyph = new Rectangle2D.Float(position.getXDirAdj(), baseline - 2,
                        position.getWidthDirAdj(), position.getHeightDir() + 2);
                assertThat(cells.stream().anyMatch(cell -> cell.contains(glyph))).isTrue();
                for (Rectangle2D.Float previous : glyphs) {
                    Rectangle2D intersection = previous.createIntersection(glyph);
                    assertThat(intersection.getWidth() > 0.2 && intersection.getHeight() > 0.2).isFalse();
                }
                glyphs.add(glyph);
            }
        };
        try (PDDocument onlyPage = new PDDocument()) {
            onlyPage.importPage(page);
            stripper.getText(onlyPage);
        }
        assertThat(glyphs).isNotEmpty();
    }
}
