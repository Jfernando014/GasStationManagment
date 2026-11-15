package com.edu.unicauca.gasstation.backend.reports.infrastructure.export;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.balanceReport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

/**
 * Tests the executive balance PowerPoint export.
 *
 * @author javiersolanop777
 */
class BalancePresentationExporterTest {

    @Test
    void writesFiveSlidesWithTheOperationalSummary() throws Exception {
        byte[] bytes = new BalancePresentationExporter().export(balanceReport());

        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
            assertEquals(5, ppt.getSlides().size());
            assertTrue(text(ppt.getSlides().get(0)).contains("BALANCE OPERATIVO"));
            assertTrue(text(ppt.getSlides().get(0)).contains("Días con datos"));
            assertTrue(text(ppt.getSlides().get(1)).contains("EVOLUCIÓN DIARIA"));
            assertTrue(text(ppt.getSlides().get(2)).contains("VENTAS Y DESEMPEÑO"));
            assertTrue(text(ppt.getSlides().get(3)).contains("VARIACIÓN DE INVENTARIO"));
            assertTrue(text(ppt.getSlides().get(4)).contains("CAJA, DOMINUS Y CONCLUSIONES"));
            assertTrue(text(ppt.getSlides().get(4)).contains("Conclusiones"));
        }
    }

    private static String text(XSLFSlide slide) {
        return slide.getShapes().stream()
                .filter(shape -> shape instanceof XSLFTextShape)
                .map(shape -> ((XSLFTextShape) shape).getText())
                .collect(Collectors.joining(" "));
    }
}
