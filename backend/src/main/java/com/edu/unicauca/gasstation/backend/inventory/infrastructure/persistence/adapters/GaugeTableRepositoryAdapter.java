package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.GaugeTableRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.GaugePointEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.GaugeTableEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.GaugeTablePersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaGaugePointRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaGaugeTableRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class GaugeTableRepositoryAdapter implements GaugeTableRepository {

    private final JpaGaugeTableRepository tableRepository;
    private final JpaGaugePointRepository pointRepository;
    private final GaugeTablePersistenceMapper mapper;

    public GaugeTableRepositoryAdapter(JpaGaugeTableRepository tableRepository,
                                       JpaGaugePointRepository pointRepository,
                                       GaugeTablePersistenceMapper mapper) {
        this.tableRepository = tableRepository;
        this.pointRepository = pointRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public GaugeTable save(GaugeTable table) {
        GaugeTableEntity savedTable = tableRepository.saveAndFlush(mapper.toEntity(table));
        if (table.getId() != null) {
            pointRepository.deleteByGaugeTableId(savedTable.getId());
        }
        List<GaugePointEntity> savedPoints =
                pointRepository.saveAll(mapper.toPointEntities(savedTable.getId(), table.getPoints()));
        pointRepository.flush();
        return mapper.toDomain(savedTable, savedPoints);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GaugeTable> findById(Long id) {
        return tableRepository.findById(id).map(entity ->
                mapper.toDomain(entity, pointRepository.findByGaugeTableIdOrderByHeightCm(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GaugeTable> findAllOrderByName() {
        List<GaugeTableEntity> tables = tableRepository.findAll(Sort.by("name"));
        if (tables.isEmpty()) {
            return List.of();
        }
        Map<Long, List<GaugePointEntity>> pointsByTable = pointRepository
                .findByGaugeTableIdInOrderByGaugeTableIdAscHeightCmAsc(
                        tables.stream().map(GaugeTableEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(GaugePointEntity::getGaugeTableId));
        return tables.stream()
                .map(table -> mapper.toDomain(table, pointsByTable.getOrDefault(table.getId(), List.of())))
                .toList();
    }

    @Override
    public boolean existsById(Long id) {
        return tableRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return tableRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return tableRepository.existsByNameAndIdNot(name, id);
    }
}