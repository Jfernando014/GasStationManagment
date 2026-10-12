package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.GaugePointEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.GaugeTableEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GaugeTablePersistenceMapper {

    public GaugeTable toDomain(GaugeTableEntity entity, List<GaugePointEntity> pointEntities) {
        GaugeTable table = new GaugeTable();
        table.setId(entity.getId());
        table.setName(entity.getName());
        List<GaugePoint> points = new ArrayList<>(pointEntities.size());
        for (GaugePointEntity pointEntity : pointEntities) {
            points.add(new GaugePoint(pointEntity.getHeightCm(), pointEntity.getVolumeGallons()));
        }
        table.setPoints(points);
        return table;
    }

    public GaugeTableEntity toEntity(GaugeTable table) {
        GaugeTableEntity entity = new GaugeTableEntity();
        entity.setId(table.getId());
        entity.setName(table.getName());
        return entity;
    }

    public List<GaugePointEntity> toPointEntities(Long gaugeTableId, List<GaugePoint> points) {
        List<GaugePointEntity> entities = new ArrayList<>(points.size());
        for (GaugePoint point : points) {
            GaugePointEntity entity = new GaugePointEntity();
            entity.setGaugeTableId(gaugeTableId);
            entity.setHeightCm(point.getHeightCm());
            entity.setVolumeGallons(point.getVolumeGallons());
            entities.add(entity);
        }
        return entities;
    }
}