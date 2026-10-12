package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.assertInvalidGauge;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.curve;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.point;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.table;
import static org.assertj.core.api.Assertions.assertThat;

class GaugeCsvParserTest {

    private final GaugeCsvParser parser = new GaugeCsvParser();

    // ---------- parse: formato ----------

    @Test
    void parsesCommaSeparatedValuesWithHeader() {
        List<GaugePoint> points = parser.parse("Altura_cm,Volumen_Galones\n1,1\n2,4\n");

        assertThat(points).hasSize(2);
        assertThat(points.get(1).getHeightCm()).isEqualByComparingTo("2");
        assertThat(points.get(1).getVolumeGallons()).isEqualByComparingTo("4");
    }

    @Test
    void parsesWithoutHeader() {
        assertThat(parser.parse("1,1\n2,4")).hasSize(2);
    }

    @Test
    void parsesWindowsLineEndings() {
        assertThat(parser.parse("Altura_cm,Volumen_Galones\r\n1,1\r\n2,4\r\n")).hasSize(2);
    }

    @Test
    void parsesSemicolonSeparatedValuesWithDecimalComma() {
        List<GaugePoint> points = parser.parse("10,5;100,25\n11;110");

        assertThat(points.get(0).getHeightCm()).isEqualByComparingTo("10.5");
        assertThat(points.get(0).getVolumeGallons()).isEqualByComparingTo("100.25");
        assertThat(points.get(1).getHeightCm()).isEqualByComparingTo("11");
    }

    @Test
    void parsesTabSeparatedValuesWithDecimalCommaAndHeader() {
        List<GaugePoint> points = parser.parse("Altura\tVolumen\n10,5\t100,25\n");

        assertThat(points).hasSize(1);
        assertThat(points.get(0).getHeightCm()).isEqualByComparingTo("10.5");
    }

    @Test
    void parsesCommaSeparatedValuesWithDotDecimals() {
        List<GaugePoint> points = parser.parse("10.5,100.25\n11,110");

        assertThat(points.get(0).getHeightCm()).isEqualByComparingTo("10.5");
        assertThat(points.get(0).getVolumeGallons()).isEqualByComparingTo("100.25");
    }

    @Test
    void stripsTheByteOrderMarkAddedByExcel() {
        assertThat(parser.parse("\uFEFFAltura_cm,Volumen_Galones\n1,1\n2,4")).hasSize(2);
    }

    @Test
    void returnsNoPointsForNullEmptyOrHeaderOnlyContent() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("Altura_cm,Volumen_Galones\n")).isEmpty();
    }

    // ---------- parse: errores ----------

    @Test
    void rejectsDecimalCommasWhenTheSeparatorIsAComma() {
        // "10,5,100" son tres columnas: no se adivina qué coma es decimal
        assertInvalidGauge(() -> parser.parse("10,5,100"), "line[1]");
    }

    @Test
    void rejectsALineWithANonNumericValue() {
        assertInvalidGauge(() -> parser.parse("1,1\n2,xyz"), "line[2]");
    }

    @Test
    void rejectsALineWithASingleValue() {
        assertInvalidGauge(() -> parser.parse("1,1\n2"), "line[2]");
    }

    @Test
    void keepsLineNumbersWhenThereAreBlankLines() {
        assertInvalidGauge(() -> parser.parse("1,1\n\nabc,4"), "line[3]");
    }

    @Test
    void doesNotReorderTheRowsSoAnUnorderedFileIsRejectedByTheTable() {
        List<GaugePoint> points = parser.parse("5,16\n1,1");

        assertThat(points.get(0).getHeightCm()).isEqualByComparingTo("5");
        GaugeTable table = new GaugeTable();
        table.setPoints(points);
        assertInvalidGauge(table::validate, "points[1]");
    }

    // ---------- export ----------

    @Test
    void exportWritesTheHeaderAndTheRowsWithoutTrailingZeros() {
        GaugeTable table = table(1L, "Curva", point("1.0", "1.00"), point("55.5", "566.50"), point("100.0", "1000.00"));

        assertThat(parser.export(table))
                .isEqualTo("Altura_cm,Volumen_Galones\n1,1\n55.5,566.5\n100,1000\n");
    }

    @Test
    void exportedFileCanBeImportedAgainUnchanged() {
        GaugeTable original = curve(1L, "Curva");

        List<GaugePoint> imported = parser.parse(parser.export(original));

        assertThat(imported).hasSameSizeAs(original.getPoints());
        for (int i = 0; i < imported.size(); i++) {
            assertThat(imported.get(i).getHeightCm()).isEqualByComparingTo(original.getPoints().get(i).getHeightCm());
            assertThat(imported.get(i).getVolumeGallons())
                    .isEqualByComparingTo(original.getPoints().get(i).getVolumeGallons());
        }
    }
}