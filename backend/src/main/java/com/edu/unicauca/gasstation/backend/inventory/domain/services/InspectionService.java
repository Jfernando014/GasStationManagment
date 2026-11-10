package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InspectionRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateInspectionCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InspectionNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidInspectionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final Clock clock;

    @Transactional
    public Inspection create(Inspection inspection) {
        normalize(inspection);
        validate(inspection, null);

        LocalDateTime now = LocalDateTime.now(clock);
        inspection.setId(null);
        inspection.setCreatedAt(now);
        inspection.setUpdatedAt(now);
        return withStatus(inspectionRepository.save(inspection));
    }

    @Transactional
    public Inspection update(Long id, Inspection changes) {
        Inspection existing = findById(id);
        normalize(changes);
        validate(changes, id);

        existing.setCode(changes.getCode());
        existing.setCategory(changes.getCategory());
        existing.setTarget(changes.getTarget());
        existing.setDateExecuted(changes.getDateExecuted());
        existing.setDateDue(changes.getDateDue());
        existing.setCertifier(changes.getCertifier());
        existing.setToleranceMeasured(changes.getToleranceMeasured());
        existing.setCertDoc(changes.getCertDoc());
        existing.setUpdatedAt(LocalDateTime.now(clock));
        return withStatus(inspectionRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public Inspection findById(Long id) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new InspectionNotFoundException(id));
        return withStatus(inspection);
    }

    /**
     * Filters by category and execution date in the repository; the regulatory status depends on
     * today's date, so it is applied here once the status of each inspection is known.
     */
    @Transactional(readOnly = true)
    public List<Inspection> search(InspectionCategory category, InspectionStatus status,
                                   LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidInspectionException("The start date cannot be after the end date");
        }
        LocalDate today = LocalDate.now(clock);
        return inspectionRepository.search(category, from, to).stream()
                .map(inspection -> withStatus(inspection, today))
                .filter(inspection -> status == null || inspection.getStatus() == status)
                .toList();
    }

    private Inspection withStatus(Inspection inspection) {
        return withStatus(inspection, LocalDate.now(clock));
    }

    private Inspection withStatus(Inspection inspection, LocalDate today) {
        inspection.setStatus(inspection.statusOn(today));
        return inspection;
    }

    private void normalize(Inspection inspection) {
        inspection.setCode(inspection.getCode().trim());
        inspection.setTarget(inspection.getTarget().trim());
        inspection.setCertifier(inspection.getCertifier().trim());
        inspection.setToleranceMeasured(inspection.getToleranceMeasured().trim());
        String certDoc = inspection.getCertDoc();
        inspection.setCertDoc(certDoc == null || certDoc.isBlank() ? null : certDoc.trim());
    }

    private void validate(Inspection inspection, Long excludedId) {
        if (inspection.getDateExecuted().isAfter(LocalDate.now(clock))) {
            throw new InvalidInspectionException("The execution date is not valid");
        }
        if (!inspection.getDateDue().isAfter(inspection.getDateExecuted())) {
            throw new InvalidInspectionException("The due date must be after the execution date");
        }
        boolean duplicated = excludedId == null
                ? inspectionRepository.existsByCode(inspection.getCode())
                : inspectionRepository.existsByCodeAndIdNot(inspection.getCode(), excludedId);
        if (duplicated) {
            throw new DuplicateInspectionCodeException(inspection.getCode());
        }
    }
}
