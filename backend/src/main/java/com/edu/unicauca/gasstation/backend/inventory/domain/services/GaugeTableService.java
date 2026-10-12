package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.GaugeTableRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateGaugeTableNameException;
import com.edu.unicauca.gasstation.backend.inventory.exception.GaugeTableNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GaugeTableService {

    private final GaugeTableRepository gaugeTableRepository;
    private final TankRepository tankRepository;
    private final GaugeCsvParser csvParser;
    private final BigDecimal deadVolumeGallons;

    public GaugeTableService(GaugeTableRepository gaugeTableRepository,
                             TankRepository tankRepository,
                             GaugeCsvParser csvParser,
                             @Value("${inventory.gauge.dead-volume-gallons:20}") BigDecimal deadVolumeGallons) {
        this.gaugeTableRepository = gaugeTableRepository;
        this.tankRepository = tankRepository;
        this.csvParser = csvParser;
        this.deadVolumeGallons = deadVolumeGallons;
    }

    @Transactional
    public GaugeTable create(GaugeTable table) {
        table.setName(table.getName().trim());
        table.validate();
        if (gaugeTableRepository.existsByName(table.getName())) {
            throw new DuplicateGaugeTableNameException(table.getName());
        }
        return gaugeTableRepository.save(table);
    }

    @Transactional
    public GaugeTable createFromCsv(String name, String csv) {
        GaugeTable table = new GaugeTable();
        table.setName(name);
        table.setPoints(csvParser.parse(csv));
        return create(table);
    }

    @Transactional
    public GaugeTable update(Long id, GaugeTable changes) {
        return replace(findById(id), changes.getName(), changes);
    }

    @Transactional
    public GaugeTable replacePointsFromCsv(Long id, String csv) {
        GaugeTable current = findById(id);
        GaugeTable changes = new GaugeTable();
        changes.setPoints(csvParser.parse(csv));
        return replace(current, current.getName(), changes);
    }

    @Transactional(readOnly = true)
    public GaugeTable findById(Long id) {
        return gaugeTableRepository.findById(id).orElseThrow(() -> new GaugeTableNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<GaugeTable> findAll() {
        return gaugeTableRepository.findAllOrderByName();
    }

    @Transactional(readOnly = true)
    public List<Tank> findTanksUsing(Long gaugeTableId) {
        return tankRepository.findByGaugeTableId(gaugeTableId);
    }

    public String toCsv(GaugeTable table) {
        return csvParser.export(table);
    }

    /**
     * Galones del tanque a una altura medida con la regla: curva certificada interpolada menos
     * el volumen muerto configurado, nunca menor que 0. El descuento se aplica aquí y no en la
     * tabla para que el CSV importado y exportado sea igual a la curva certificada.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateVolume(Long tankId, BigDecimal heightCm) {
        Tank tank = tankRepository.findById(tankId).orElseThrow(() -> new TankNotFoundException(tankId));
        if (tank.getGaugeTableId() == null) {
            throw new TankWithoutGaugeTableException(tankId);
        }
        GaugeTable table = findById(tank.getGaugeTableId());
        return table.volumeAt(heightCm).subtract(deadVolumeGallons).max(BigDecimal.ZERO);
    }

    private GaugeTable replace(GaugeTable current, String name, GaugeTable changes) {
        changes.setName(name.trim());
        changes.validate();
        if (gaugeTableRepository.existsByNameAndIdNot(changes.getName(), current.getId())) {
            throw new DuplicateGaugeTableNameException(changes.getName());
        }
        current.setName(changes.getName());
        current.setPoints(changes.getPoints());
        return gaugeTableRepository.save(current);
    }
}