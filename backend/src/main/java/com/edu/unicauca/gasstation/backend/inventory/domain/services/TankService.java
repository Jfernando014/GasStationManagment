package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateTankCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TankService {
    private final TankRepository tankRepository;

    public TankService(TankRepository tankRepository) {
        this.tankRepository = tankRepository;
    }

    @Transactional
    public Tank createTank(Tank tank) {
        if (tankRepository.existsByCode(tank.getCode())) {
            throw new DuplicateTankCodeException(tank.getCode());
        }
        return tankRepository.save(tank);
    }

    @Transactional
    public Tank findTankById(Long id) {
        return tankRepository.findById(id).orElseThrow(() -> new TankNotFoundException(id));
    }

    @Transactional
    public List<Tank> findAllTanks() {
        return tankRepository.findAllOrderByCode();
    }
    @Transactional
    public Tank updateTank(Long id, Tank changes) {

        Tank tank = findTankById(id);
        if (!tank.getCode().equals(changes.getCode())
                && tankRepository.existsByCode(changes.getCode())) {
            throw new DuplicateTankCodeException(changes.getCode());
        }
        tank.setCode(changes.getCode());
        tank.setName(changes.getName());
        tank.setFuelType(changes.getFuelType());
        tank.setMaxCapacityGallons(changes.getMaxCapacityGallons());
        tank.setMaxHeightCm(changes.getMaxHeightCm());
        tank.setToleranceCm(changes.getToleranceCm());
        return tankRepository.save(tank);
    }
}
