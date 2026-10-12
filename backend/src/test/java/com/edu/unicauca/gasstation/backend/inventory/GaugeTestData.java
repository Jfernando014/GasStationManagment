package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class GaugeTestData {

    private GaugeTestData() {
    }

    public static GaugePoint point(String heightCm, String volumeGallons) {
        return new GaugePoint(new BigDecimal(heightCm), new BigDecimal(volumeGallons));
    }

    public static GaugeTable table(Long id, String name, GaugePoint... points) {
        GaugeTable table = new GaugeTable();
        table.setId(id);
        table.setName(name);
        table.setPoints(new ArrayList<>(List.of(points)));
        return table;
    }

    /** Puntos reales de la curva T1/T2 del cliente (1, 5, 6, 54, 55, 56 y 238 cm). A 55 cm: 559 gal. */
    public static GaugeTable curve(Long id, String name) {
        return table(id, name,
                point("1", "1"), point("5", "16"), point("6", "21"),
                point("54", "544"), point("55", "559"), point("56", "574"),
                point("238", "3090"));
    }

    /** Comprueba que la acción lanza InvalidGaugeTableException señalando el campo esperado. */
    public static void assertInvalidGauge(ThrowingCallable action, String expectedField) {
        assertThatThrownBy(action).isInstanceOfSatisfying(InvalidGaugeTableException.class,
                e -> assertThat(e.getField()).isEqualTo(expectedField));
    }
}