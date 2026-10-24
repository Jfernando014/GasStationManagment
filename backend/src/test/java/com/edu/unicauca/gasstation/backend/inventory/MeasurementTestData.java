package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class MeasurementTestData {

    private MeasurementTestData() {
    }

    public static TankReading reading(Long tankId, String heightCm, String volumeGallons) {
        return new TankReading(tankId, new BigDecimal(heightCm),
                volumeGallons == null ? null : new BigDecimal(volumeGallons));
    }

    public static TankMeasurement measurement(Long id, LocalDate journalDate, TankReading... readings) {
        TankMeasurement measurement = new TankMeasurement();
        measurement.setId(id);
        measurement.setJournalDate(journalDate);
        measurement.setMeasuredAt(journalDate.atTime(9, 0));
        measurement.setReadings(new ArrayList<>(List.of(readings)));
        return measurement;
    }

    public static FuelReception reception(Long id, Long tankId, LocalDate date, String gallons) {
        FuelReception reception = new FuelReception();
        reception.setId(id);
        reception.setTankId(tankId);
        reception.setReceptionDate(date);
        reception.setGallons(new BigDecimal(gallons));
        reception.setSupplier("Terpel");
        reception.setCreatedAt(date.atTime(8, 0));
        reception.setUpdatedAt(date.atTime(8, 0));
        return reception;
    }

    public static Tank tankWithGauge(Long id, Long gaugeTableId) {
        Tank tank = TankTestData.tank(id, "T" + id, "Tank " + id);
        tank.setGaugeTableId(gaugeTableId);
        return tank;
    }
}