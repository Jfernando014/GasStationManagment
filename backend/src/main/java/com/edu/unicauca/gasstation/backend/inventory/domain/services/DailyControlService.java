package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.DailyControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductControl;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyControlService {

    private final FuelVariationService variationService;
    private final FuelAutonomyService autonomyService;
    private final ProductStockService productStockService;

    public DailyControl findByJournalDate(LocalDate journalDate) {
        Map<FuelType, FuelAutonomy> autonomyByFuel = autonomyService.calculate(journalDate).stream()
                .collect(Collectors.toMap(FuelAutonomy::getFuelType, Function.identity()));

        List<FuelControl> fuels = variationService.calculate(journalDate).stream()
                .map(variation -> fuelControl(journalDate, variation, autonomyByFuel.get(variation.getFuelType())))
                .toList();

        List<ProductControl> products = productStockService.findByJournalDate(journalDate, null).stream()
                .map(this::productControl)
                .toList();

        return DailyControl.builder()
                .journalDate(journalDate)
                .fuels(fuels)
                .products(products)
                .build();
    }

    private FuelControl fuelControl(LocalDate journalDate, FuelVariation variation, FuelAutonomy autonomy) {
        return FuelControl.builder()
                .fuelType(variation.getFuelType())
                .variation(variation)
                .autonomy(autonomy)
                .estimatedDepletionDate(depletionDate(journalDate, autonomy))
                .build();
    }

    private LocalDate depletionDate(LocalDate journalDate, FuelAutonomy autonomy) {
        if (autonomy == null || autonomy.getStatus() != AutonomyStatus.CALCULATED) {
            return null;
        }
        long wholeDays = autonomy.getAutonomyDays().setScale(0, RoundingMode.DOWN).longValue();
        return journalDate.plusDays(wholeDays);
    }

    private ProductControl productControl(ProductStock stock) {
        int office = stock.getOffice().getClosing();
        int counter = stock.getCounter().getClosing();
        return ProductControl.builder()
                .productId(stock.getProductId())
                .productCode(stock.getProductCode())
                .productName(stock.getProductName())
                .category(stock.getCategory())
                .sold(stock.getCounter().getSold())
                .officeStock(office)
                .counterStock(counter)
                .status(stock.getStatus())
                .depleted(office + counter <= 0)
                .build();
    }
}