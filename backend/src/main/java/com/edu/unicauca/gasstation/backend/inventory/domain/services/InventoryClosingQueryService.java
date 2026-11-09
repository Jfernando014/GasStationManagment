package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingQuery;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot.AlertData;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot.FuelVariationData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InventoryAlertRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Reads the inventory state of a journal date and publishes it through {@link InventoryClosingQuery} (S3-19).
 */
@Service
@RequiredArgsConstructor
public class InventoryClosingQueryService implements InventoryClosingQuery {

    private final TankMeasurementRepository measurementRepository;
    private final FuelVariationService variationService;
    private final InventoryAlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryClosingSnapshot findClosingSnapshot(LocalDate journalDate) {
        if (journalDate == null) {
            return new InventoryClosingSnapshot(false, List.of(), List.of());
        }

        boolean hasMeasurement = measurementRepository.existsByJournalDate(journalDate);
        List<FuelVariationData> variations = variationService.calculate(journalDate).stream()
                .map(InventoryClosingQueryService::toVariationData)
                .toList();
        List<AlertData> alerts = alertRepository.findByOpenedOn(journalDate).stream()
                .map(InventoryClosingQueryService::toAlertData)
                .toList();

        return new InventoryClosingSnapshot(hasMeasurement, variations, alerts);
    }

    private static FuelVariationData toVariationData(FuelVariation variation) {
        return new FuelVariationData(
                variation.getFuelType().name(),
                variation.getInitialGallons(),
                variation.getEntriesGallons(),
                variation.getSoldGallons(),
                variation.getTheoreticalGallons(),
                variation.getMeasuredGallons(),
                variation.getVarianceGallons(),
                variation.getToleranceGallons(),
                variation.isOutOfTolerance(),
                variation.getStatus().name()
        );
    }

    private static AlertData toAlertData(InventoryAlert alert) {
        return new AlertData(
                alert.getId(),
                alert.getAlertType().name(),
                alert.getFuelType().name(),
                alert.getLevel().name(),
                alert.getStatus().name(),
                alert.getAutonomyDays(),
                alert.getOpenedOn()
        );
    }
}
