package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InventoryAlertRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidInventoryAlertException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InventoryAlertConflictException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InventoryAlertNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryAlertService {

    private static final int NOTE_MAX_LENGTH = 200;
    private static final List<AlertStatus> PENDING = List.of(AlertStatus.ACTIVE, AlertStatus.READ);
    private static final List<AlertStatus> OPEN = List.of(AlertStatus.ACTIVE, AlertStatus.READ, AlertStatus.ATTENDED);

    private final InventoryAlertRepository repository;
    private final FuelAutonomyService autonomyService;
    private final Clock clock;


    public void reconcile(LocalDate journalDate) {
        if (journalDate == null) {
            throw new InvalidInventoryAlertException("The journal date is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (FuelAutonomy autonomy : autonomyService.calculate(journalDate)) {
            if (!canBeEvaluated(autonomy)) {
                continue;
            }
            Optional<InventoryAlert> open = repository.findOpen(AlertType.AUTONOMY, autonomy.getFuelType());
            if (autonomy.getLevel() == AutonomyLevel.OK) {
                open.ifPresent(alert -> resolve(alert, now));
            } else if (open.isPresent()) {
                refresh(open.get(), autonomy);
            } else {
                open(autonomy, journalDate, now);
            }
        }
    }

    public List<InventoryAlert> findAll(AlertStatus status) {
        Collection<AlertStatus> statuses = status == null ? OPEN : List.of(status);
        List<InventoryAlert> alerts = new ArrayList<>(repository.findByStatuses(statuses));
        alerts.sort(Comparator.comparingInt(alert -> alert.getLevel() == AutonomyLevel.CRITICAL ? 0 : 1));
        return alerts;
    }

    public long countPending() {
        return repository.countByStatuses(PENDING);
    }

    public InventoryAlert markRead(Long id) {
        InventoryAlert alert = find(id);
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            return alert;
        }
        alert.setStatus(AlertStatus.READ);
        alert.setReadAt(LocalDateTime.now(clock));
        return repository.save(alert);
    }

    public InventoryAlert attend(Long id, String note) {
        String cleanNote = optionalNote(note);
        InventoryAlert alert = find(id);
        if (!alert.isOpen()) {
            throw new InventoryAlertConflictException("The alert is already resolved");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (alert.getReadAt() == null) {
            alert.setReadAt(now);
        }
        alert.setStatus(AlertStatus.ATTENDED);
        alert.setAttendedAt(now);
        alert.setAttendNote(cleanNote);
        return repository.save(alert);
    }


    private boolean canBeEvaluated(FuelAutonomy autonomy) {
        return autonomy.getStatus() == AutonomyStatus.CALCULATED
                && autonomy.getLevel() != AutonomyLevel.NOT_EVALUATED;
    }

    private void open(FuelAutonomy autonomy, LocalDate journalDate, LocalDateTime now) {
        InventoryAlert alert = new InventoryAlert();
        alert.setAlertType(AlertType.AUTONOMY);
        alert.setFuelType(autonomy.getFuelType());
        alert.setLevel(autonomy.getLevel());
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setAutonomyDays(autonomy.getAutonomyDays());
        alert.setOpenedOn(journalDate);
        alert.setCreatedAt(now);
        repository.save(alert);
    }

    private void refresh(InventoryAlert alert, FuelAutonomy autonomy) {
        boolean sameLevel = alert.getLevel() == autonomy.getLevel();
        boolean sameDays = alert.getAutonomyDays().compareTo(autonomy.getAutonomyDays()) == 0;
        if (sameLevel && sameDays) {
            return;
        }
        boolean escalated = autonomy.getLevel() == AutonomyLevel.CRITICAL
                && alert.getLevel() != AutonomyLevel.CRITICAL;
        alert.setLevel(autonomy.getLevel());
        alert.setAutonomyDays(autonomy.getAutonomyDays());
        if (escalated) {
            // It got worse: the administrator must see it again
            alert.setStatus(AlertStatus.ACTIVE);
            alert.setReadAt(null);
            alert.setAttendedAt(null);
            alert.setAttendNote(null);
        }
        repository.save(alert);
    }

    private void resolve(InventoryAlert alert, LocalDateTime now) {
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(now);
        repository.save(alert);
    }


    private InventoryAlert find(Long id) {
        return repository.findById(id).orElseThrow(() -> new InventoryAlertNotFoundException(id));
    }

    private String optionalNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.length() > NOTE_MAX_LENGTH) {
            throw new InvalidInventoryAlertException("The note cannot exceed 200 characters");
        }
        return trimmed;
    }
}