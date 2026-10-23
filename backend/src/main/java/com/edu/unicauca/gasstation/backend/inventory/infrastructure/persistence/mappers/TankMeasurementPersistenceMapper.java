package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankMeasurementEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankReadingEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TankMeasurementPersistenceMapper {

    public TankMeasurementEntity toEntity(TankMeasurement measurement) {
        TankMeasurementEntity entity = new TankMeasurementEntity();
        entity.setId(measurement.getId());
        entity.setJournalDate(measurement.getJournalDate());
        entity.setMeasuredAt(measurement.getMeasuredAt());
        return entity;
    }

    public TankReadingEntity toReadingEntity(TankReading reading, Long measurementId) {
        TankReadingEntity entity = new TankReadingEntity();
        entity.setMeasurementId(measurementId);
        entity.setTankId(reading.getTankId());
        entity.setHeightCm(reading.getHeightCm());
        entity.setVolumeGallons(reading.getVolumeGallons());
        return entity;
    }

    public TankMeasurement toDomain(TankMeasurementEntity entity, List<TankReadingEntity> readings) {
        TankMeasurement measurement = new TankMeasurement();
        measurement.setId(entity.getId());
        measurement.setJournalDate(entity.getJournalDate());
        measurement.setMeasuredAt(entity.getMeasuredAt());
        List<TankReading> mapped = new ArrayList<>();
        for (TankReadingEntity r : readings) {
            mapped.add(new TankReading(r.getTankId(), r.getHeightCm(), r.getVolumeGallons()));
        }
        measurement.setReadings(mapped);
        return measurement;
    }
}