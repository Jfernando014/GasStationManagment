package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelReceptionRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelReceptionNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidFuelReceptionException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ReceptionExceedsCapacityException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FuelReceptionService {

    private final FuelReceptionRepository receptionRepository;
    private final TankRepository tankRepository;
    private final TankMeasurementRepository measurementRepository;
    private final Clock clock;

    @Transactional
    public FuelReception create(FuelReception reception) {
        Tank tank = requireTank(reception.getTankId());
        reception.setSupplier(reception.getSupplier().trim());
        validate(reception, tank, null);

        LocalDateTime now = LocalDateTime.now(clock);
        reception.setId(null);
        reception.setCreatedAt(now);
        reception.setUpdatedAt(now);
        return receptionRepository.save(reception);
    }

    @Transactional
    public FuelReception update(Long id, FuelReception changes) {
        FuelReception existing = findById(id);
        Tank tank = requireTank(changes.getTankId());
        changes.setSupplier(changes.getSupplier().trim());
        validate(changes, tank, id);

        existing.setTankId(changes.getTankId());
        existing.setReceptionDate(changes.getReceptionDate());
        existing.setGallons(changes.getGallons());
        existing.setSupplier(changes.getSupplier());
        existing.setUpdatedAt(LocalDateTime.now(clock));
        return receptionRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public FuelReception findById(Long id) {
        return receptionRepository.findById(id).orElseThrow(() -> new FuelReceptionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<FuelReception> search(Long tankId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidFuelReceptionException("The start date cannot be after the end date");
        }
        return receptionRepository.search(tankId, from, to);
    }

    private Tank requireTank(Long tankId) {
        return tankRepository.findById(tankId).orElseThrow(() -> new TankNotFoundException(tankId));
    }

    private void validate(FuelReception reception, Tank tank, Long excludedReceptionId) {
        if (reception.getReceptionDate().isAfter(LocalDate.now(clock))) {
            throw new InvalidFuelReceptionException("The reception date is not valid");
        }
        BigDecimal projected = stockAffectedBy(reception, excludedReceptionId).add(reception.getGallons());
        if (projected.compareTo(tank.getMaxCapacityGallons()) > 0) {
            throw new ReceptionExceedsCapacityException();
        }
    }


    private BigDecimal stockAffectedBy(FuelReception reception, Long excludedReceptionId) {
        Optional<TankMeasurement> latest = measurementRepository.findLatestContainingTank(reception.getTankId());
        if (latest.isEmpty() || !reception.getReceptionDate().isAfter(latest.get().getJournalDate())) {
            return BigDecimal.ZERO;
        }
        BigDecimal measured = latest.get().readingOf(reception.getTankId())
                .map(TankReading::getVolumeGallons)
                .orElse(BigDecimal.ZERO);
        BigDecimal received = receptionRepository.sumGallonsAfter(
                reception.getTankId(), latest.get().getJournalDate(), excludedReceptionId);
        return measured.add(received);
    }
}