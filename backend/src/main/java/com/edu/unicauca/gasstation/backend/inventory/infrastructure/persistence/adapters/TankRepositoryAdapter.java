package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.TankPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaTankRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TankRepositoryAdapter implements TankRepository {

    private final JpaTankRepository repository;
    private final TankPersistenceMapper mapper;

    public TankRepositoryAdapter(JpaTankRepository repository, TankPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Tank save(Tank tank) {
        return mapper.toDomain(repository.save(mapper.toEntity(tank)));
    }

    @Override
    public Optional<Tank> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Tank> findAllOrderByCode() {
        return repository.findAllByOrderByCodeAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }
}
