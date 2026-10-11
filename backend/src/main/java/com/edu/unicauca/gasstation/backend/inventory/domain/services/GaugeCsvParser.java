package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Convierte el CSV de la tabla de aforo en puntos y viceversa. Solo interpreta el formato:
 * las reglas de la tabla (orden, mínimo de puntos, etc.) las valida GaugeTable.
 *
 * Separador: ';' o tabulador si aparecen en la primera línea; si no, ','. La coma decimal solo
 * se acepta con ';' o tabulador; con ',' como separador los decimales llevan punto.
 */
@Component
public class GaugeCsvParser {

    private static final String HEADER = "Altura_cm,Volumen_Galones";
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    public List<GaugePoint> parse(String content) {
        List<GaugePoint> points = new ArrayList<>();
        if (content == null) {
            return points;
        }
        String[] lines = stripBom(content).split("\\R", -1);
        char separator = detectSeparator(lines);
        boolean firstContentLine = true;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String field = "line[" + (i + 1) + "]";
            String[] cells = line.split(Pattern.quote(String.valueOf(separator)), -1);

            if (firstContentLine) {
                firstContentLine = false;
                if (!NUMBER.matcher(normalize(cells[0], separator)).matches()) {
                    continue; // cabecera
                }
            }
            if (cells.length != 2) {
                throw new InvalidGaugeTableException(field,
                        "The line must contain exactly two values: height and volume");
            }
            points.add(new GaugePoint(toNumber(cells[0], separator, field), toNumber(cells[1], separator, field)));
        }
        return points;
    }

    public String export(GaugeTable table) {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (GaugePoint point : table.getPoints()) {
            csv.append(plain(point.getHeightCm())).append(',').append(plain(point.getVolumeGallons())).append('\n');
        }
        return csv.toString();
    }

    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String stripBom(String content) {
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    private char detectSeparator(String[] lines) {
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (line.contains(";")) {
                return ';';
            }
            return line.contains("\t") ? '\t' : ',';
        }
        return ',';
    }

    private String normalize(String cell, char separator) {
        String value = cell.trim();
        return separator == ',' ? value : value.replace(',', '.');
    }

    private BigDecimal toNumber(String cell, char separator, String field) {
        String value = normalize(cell, separator);
        if (!NUMBER.matcher(value).matches()) {
            throw new InvalidGaugeTableException(field, "The value '" + cell.trim() + "' is not a valid number");
        }
        return new BigDecimal(value);
    }
}