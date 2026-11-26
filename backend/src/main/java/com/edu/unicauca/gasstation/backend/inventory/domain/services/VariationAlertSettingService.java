package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GallonRange;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.VariationAlertSettingRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidVariationAlertSettingException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProtectedVariationAlertSettingException;
import com.edu.unicauca.gasstation.backend.inventory.exception.VariationAlertSettingAlreadyExistsException;
import com.edu.unicauca.gasstation.backend.inventory.exception.VariationAlertSettingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VariationAlertSettingService {

    private static final BigDecimal MAXIMUM_PERCENTAGE = new BigDecimal("100");
    private static final int MINIMUM_ESCALATION_DAYS = 1;
    private static final int MAXIMUM_ESCALATION_DAYS = 10;

    private final VariationAlertSettingRepository repository;
    private final Clock clock;

    @Transactional
    public VariationAlertSetting create(VariationAlertSetting setting) {
        validate(setting);
        if (repository.existsByValidFrom(setting.getValidFrom())) {
            throw new VariationAlertSettingAlreadyExistsException(setting.getValidFrom());
        }
        setting.setId(null);
        setting.setCreatedAt(LocalDateTime.now(clock));
        return repository.save(setting);
    }

    @Transactional
    public VariationAlertSetting update(Long id, VariationAlertSetting changes) {
        validate(changes);
        VariationAlertSetting existing = findById(id);
        if (!existing.getValidFrom().equals(changes.getValidFrom())) {
            requireNotOldest(existing);
        }
        if (repository.existsByValidFromAndIdNot(changes.getValidFrom(), id)) {
            throw new VariationAlertSettingAlreadyExistsException(changes.getValidFrom());
        }
        existing.setValidFrom(changes.getValidFrom());
        existing.setCumulativeLimitPercentage(changes.getCumulativeLimitPercentage());
        existing.setEscalationDays(changes.getEscalationDays());
        existing.setShortageLimit1To49(changes.getShortageLimit1To49());
        existing.setShortageLimit50To74(changes.getShortageLimit50To74());
        existing.setShortageLimit75To124(changes.getShortageLimit75To124());
        existing.setShortageLimit125To249(changes.getShortageLimit125To249());
        existing.setShortageLimit250To499(changes.getShortageLimit250To499());
        existing.setShortageLimit500OrMore(changes.getShortageLimit500OrMore());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        requireNotOldest(findById(id));
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public VariationAlertSetting getEffectiveOn(LocalDate date) {
        return repository.findEffectiveOn(date).orElseThrow(() -> new VariationAlertSettingNotFoundException(date));
    }

    @Transactional(readOnly = true)
    public List<VariationAlertSetting> findAll() {
        return repository.findAll();
    }

    private VariationAlertSetting findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new VariationAlertSettingNotFoundException(id));
    }

    /** The oldest setting is what keeps every month covered: it cannot be deleted or moved. */
    private void requireNotOldest(VariationAlertSetting setting) {
        boolean oldest = repository.findOldest()
                .map(first -> first.getId().equals(setting.getId()))
                .orElse(false);
        if (oldest) {
            throw new ProtectedVariationAlertSettingException();
        }
    }

    private void validate(VariationAlertSetting setting) {
        if (setting.getValidFrom() == null) {
            throw new InvalidVariationAlertSettingException("La fecha de vigencia es obligatoria");
        }
        BigDecimal percentage = setting.getCumulativeLimitPercentage();
        if (percentage == null || percentage.signum() <= 0 || percentage.compareTo(MAXIMUM_PERCENTAGE) > 0) {
            throw new InvalidVariationAlertSettingException(
                    "El porcentaje límite acumulado debe ser mayor que 0 y menor o igual a 100");
        }
        Integer days = setting.getEscalationDays();
        if (days == null || days < MINIMUM_ESCALATION_DAYS || days > MAXIMUM_ESCALATION_DAYS) {
            throw new InvalidVariationAlertSettingException("Los días seguidos deben ser un entero de 1 a 10");
        }
        validateShortageLimits(setting);
    }

    private void validateShortageLimits(VariationAlertSetting setting) {
        Integer[] limits = {
                setting.getShortageLimit1To49(), setting.getShortageLimit50To74(),
                setting.getShortageLimit75To124(), setting.getShortageLimit125To249(),
                setting.getShortageLimit250To499(), setting.getShortageLimit500OrMore()};
        for (Integer limit : limits) {
            if (limit == null || limit < 1) {
                throw new InvalidVariationAlertSettingException(
                        "Cada límite de veces por rango debe ser un entero de 1 o más");
            }
        }
        for (int range = 1; range < GallonRange.values().length; range++) {
            if (limits[range] > limits[range - 1]) {
                throw new InvalidVariationAlertSettingException(
                        "Un rango de faltante mayor no puede tener un límite mayor que el de un rango menor");
            }
        }
    }
}
