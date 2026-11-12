package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.workers.domain.models.AdjustmentDirection;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Fortnight;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeAdjustment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeCategory;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.OvertimeAdjustmentRepository;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.AdjustmentNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.InvalidAdjustmentException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Manual adjustments of the overtime sheets: hours an administrator adds or subtracts from a category. */
@Service
@RequiredArgsConstructor
public class OvertimeAdjustmentService {

    private static final int MIN_REASON_LENGTH = 3;
    private static final int MAX_REASON_LENGTH = 255;

    private final OvertimeAdjustmentRepository repository;
    private final WorkerRepository workerRepository;
    private final Clock clock;

    /**
     * Registers an adjustment for the worker in the fortnight. The category and the direction come as text so an
     * unknown one is answered with the error of this module.
     */
    public OvertimeAdjustment create(UUID workerId, YearMonth month, int fortnightNumber, String category,
                                     String direction, int hours, String reason, String createdBy) {
        Fortnight fortnight = Fortnight.of(month, fortnightNumber);
        if (workerRepository.findById(workerId).isEmpty()) {
            throw new WorkerNotFoundException(workerId);
        }
        OvertimeCategory parsedCategory = parseCategory(category);
        AdjustmentDirection parsedDirection = parseDirection(direction);
        if (hours <= 0) {
            throw new InvalidAdjustmentException("Las horas del ajuste deben ser mayores que cero");
        }
        String cleanReason = reason == null ? "" : reason.strip();
        if (cleanReason.length() < MIN_REASON_LENGTH || cleanReason.length() > MAX_REASON_LENGTH) {
            throw new InvalidAdjustmentException(
                    "El motivo debe tener entre " + MIN_REASON_LENGTH + " y " + MAX_REASON_LENGTH + " caracteres");
        }
        return repository.save(new OvertimeAdjustment(null, workerId, fortnight.from(), parsedCategory,
                parsedDirection, hours, cleanReason, Instant.now(clock), createdBy));
    }

    /** Adjustments of the worker in the fortnight, oldest first. */
    public List<OvertimeAdjustment> list(UUID workerId, YearMonth month, int fortnightNumber) {
        Fortnight fortnight = Fortnight.of(month, fortnightNumber);
        if (workerRepository.findById(workerId).isEmpty()) {
            throw new WorkerNotFoundException(workerId);
        }
        return repository.findByWorkerAndFortnight(workerId, fortnight.from());
    }

    public void delete(Long id) {
        if (repository.findById(id).isEmpty()) {
            throw new AdjustmentNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private static OvertimeCategory parseCategory(String category) {
        try {
            return OvertimeCategory.valueOf(category == null ? "" : category.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAdjustmentException("La categoría no existe; use "
                    + String.join(", ", Arrays.stream(OvertimeCategory.values()).map(Enum::name).toList()));
        }
    }

    private static AdjustmentDirection parseDirection(String direction) {
        try {
            return AdjustmentDirection.valueOf(direction == null ? "" : direction.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAdjustmentException("El tipo de ajuste debe ser ADD (más) o SUBTRACT (menos)");
        }
    }
}
