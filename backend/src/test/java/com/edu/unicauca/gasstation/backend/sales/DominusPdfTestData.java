package com.edu.unicauca.gasstation.backend.sales;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class DominusPdfTestData {
    private DominusPdfTestData() { }

    public static DominusCashComplement sample() throws IOException {
        return mapper().readValue(sampleJson(), DominusCashComplement.class);
    }

    public static byte[] sampleJson() throws IOException {
        try (InputStream input = DominusPdfTestData.class.getResourceAsStream("/dominus/cash-complement.json")) {
            if (input == null) {
                throw new IOException("Missing Dominus sample fixture");
            }
            return input.readAllBytes();
        }
    }

    public static ObjectMapper mapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public static byte[] originalPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 1; i <= 2; i++) {
                PDPage page = new PDPage(i == 1 ? PDRectangle.A4 : PDRectangle.LETTER);
                if (i == 2) {
                    page.setRotation(90);
                }
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
                    stream.newLineAtOffset(40, 500);
                    stream.showText("Original Dominus sample - page " + i);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
