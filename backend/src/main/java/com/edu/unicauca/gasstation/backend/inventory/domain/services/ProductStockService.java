package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.LocationStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ProductStock;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.SalesSyncResult;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockBalance;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.ProductRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.StockMovementRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidStockMovementException;
import com.edu.unicauca.gasstation.backend.inventory.exception.NoSalesDataException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProductNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.StockConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private static final int NOTE_MAX_LENGTH = 200;

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final SalesSummaryPort salesSummaryPort;
    private final Clock clock;

    // ---------- movements ----------

    /**
     * Opening stock of both locations, saved together or not at all.
     */
    public List<StockMovement> registerInitial(Long productId, int officeQuantity, int counterQuantity,
                                               LocalDate date) {
        requireUsableProduct(productId);
        requireValidDate(date);
        if (officeQuantity < 0 || counterQuantity < 0) {
            throw new InvalidStockMovementException(
                    "The initial stock must be a whole number greater than or equal to zero");
        }
        if (movementRepository.existsInitial(productId, StockLocation.OFFICE)
                || movementRepository.existsInitial(productId, StockLocation.COUNTER)) {
            throw new StockConflictException("The initial stock of the product was already registered");
        }
        return movementRepository.saveAll(List.of(
                movement(productId, date, StockLocation.OFFICE, StockMovementType.INITIAL, officeQuantity, null),
                movement(productId, date, StockLocation.COUNTER, StockMovementType.INITIAL, counterQuantity, null)));
    }

    /**
     * Purchases arrive at the office.
     */
    public StockMovement registerEntry(Long productId, int quantity, LocalDate date, String note) {
        requireUsableProduct(productId);
        requireValidDate(date);
        if (quantity <= 0) {
            throw new InvalidStockMovementException("The entry quantity must be greater than zero");
        }
        StockMovement entry = movement(productId, date, StockLocation.OFFICE, StockMovementType.ENTRY,
                quantity, optionalNote(note));
        return movementRepository.saveAll(List.of(entry)).get(0);
    }

    /**
     * Moves units from the office to the counter as two ledger lines saved together.
     */
    public List<StockMovement> registerTransfer(Long productId, int quantity, LocalDate date, String note) {
        requireUsableProduct(productId);
        requireValidDate(date);
        if (quantity <= 0) {
            throw new InvalidStockMovementException("The transfer quantity must be greater than zero");
        }
        String cleanNote = optionalNote(note);
        if (movementRepository.sumQuantityUpTo(productId, StockLocation.OFFICE, date) < quantity) {
            throw new StockConflictException("The office does not have enough units for the transfer");
        }
        return movementRepository.saveAll(List.of(
                movement(productId, date, StockLocation.OFFICE, StockMovementType.TRANSFER, -quantity, cleanNote),
                movement(productId, date, StockLocation.COUNTER, StockMovementType.TRANSFER, quantity, cleanNote)));
    }

    /**
     * Signed correction of a location. The reason is mandatory and the stock cannot go below zero.
     */
    public StockMovement registerAdjustment(Long productId, StockLocation location, int quantity,
                                            LocalDate date, String note) {
        requireUsableProduct(productId);
        requireValidDate(date);
        if (location == null) {
            throw new InvalidStockMovementException("The location is required");
        }
        if (quantity == 0) {
            throw new InvalidStockMovementException("The adjustment cannot be zero");
        }
        String reason = optionalNote(note);
        if (reason == null) {
            throw new InvalidStockMovementException("The reason of the adjustment is required");
        }
        if (quantity < 0 && movementRepository.sumQuantityUpTo(productId, location, date) + quantity < 0) {
            throw new StockConflictException("The adjustment would leave the stock below zero");
        }
        StockMovement adjustment = movement(productId, date, location, StockMovementType.ADJUSTMENT,
                quantity, reason);
        return movementRepository.saveAll(List.of(adjustment)).get(0);
    }

    // ---------- sales of step 2 ----------

    /**
     * Replaces the SALE movements of the date with the units sold in step 2. It can be repeated
     * whenever the sales are corrected. Sold codes that are not in the catalog are reported.
     * A sale is a fact, so it is never rejected for leaving the counter negative.
     */
    public SalesSyncResult syncSales(LocalDate date) {
        requireValidDate(date);
        SalesSummary summary = salesSummaryPort.findByJournalDate(date)
                .orElseThrow(() -> new NoSalesDataException(date));
        Map<String, Product> productsByCode = productRepository.findAll(null, null).stream()
                .collect(Collectors.toMap(Product::getCode, Function.identity()));

        List<StockMovement> sales = new ArrayList<>();
        Set<String> unmatched = new TreeSet<>();
        summary.unitsByProduct().forEach((code, units) -> {
            Product product = productsByCode.get(code);
            if (product == null) {
                unmatched.add(code);
            } else if (units > 0) {
                sales.add(movement(product.getId(), date, StockLocation.COUNTER, StockMovementType.SALE,
                        -units, null));
            }
        });
        movementRepository.replaceSales(date, sales);
        return new SalesSyncResult(date, sales.size(), List.copyOf(unmatched));
    }

    // ---------- day report ----------

    /**
     * Stock of each product on a journal date: the opening comes from the movements before it and
     * the closing adds the movements of the day. Inactive products appear only if they have stock
     * or movements.
     */
    public List<ProductStock> findByJournalDate(LocalDate date, ProductCategory category) {
        Map<StockKey, Integer> opening = movementRepository.findBalancesBefore(date).stream()
                .collect(Collectors.toMap(balance -> new StockKey(balance.getProductId(), balance.getLocation()),
                        StockBalance::getQuantity));
        Map<Long, List<StockMovement>> movementsByProduct = movementRepository.findByJournalDate(date).stream()
                .collect(Collectors.groupingBy(StockMovement::getProductId));

        return productRepository.findAll(null, null).stream()
                .filter(product -> category == null || product.getCategory() == category)
                .map(product -> productStock(product, opening,
                        movementsByProduct.getOrDefault(product.getId(), List.of())))
                .filter(stock -> stock.isActive()
                        || stock.getOffice().hasActivity() || stock.getCounter().hasActivity())
                .toList();
    }

    private ProductStock productStock(Product product, Map<StockKey, Integer> opening,
                                      List<StockMovement> movements) {
        LocationStock office = locationStock(
                opening.getOrDefault(new StockKey(product.getId(), StockLocation.OFFICE), 0),
                movements, StockLocation.OFFICE);
        LocationStock counter = locationStock(
                opening.getOrDefault(new StockKey(product.getId(), StockLocation.COUNTER), 0),
                movements, StockLocation.COUNTER);
        boolean negative = office.getClosing() < 0 || counter.getClosing() < 0;
        return ProductStock.builder()
                .productId(product.getId())
                .productCode(product.getCode())
                .productName(product.getName())
                .category(product.getCategory())
                .active(product.getActive())
                .office(office)
                .counter(counter)
                .status(negative ? StockStatus.NEGATIVE_STOCK : StockStatus.OK)
                .build();
    }

    private LocationStock locationStock(int opening, List<StockMovement> movements, StockLocation location) {
        int initial = sum(movements, location, StockMovementType.INITIAL);
        int entries = sum(movements, location, StockMovementType.ENTRY);
        int transfers = sum(movements, location, StockMovementType.TRANSFER);
        int adjustments = sum(movements, location, StockMovementType.ADJUSTMENT);
        int sold = -sum(movements, location, StockMovementType.SALE);
        return LocationStock.builder()
                .opening(opening)
                .initial(initial)
                .entries(entries)
                .transfers(transfers)
                .adjustments(adjustments)
                .sold(sold)
                .closing(opening + initial + entries + transfers + adjustments - sold)
                .build();
    }

    private int sum(List<StockMovement> movements, StockLocation location, StockMovementType type) {
        return movements.stream()
                .filter(movement -> movement.getLocation() == location && movement.getMovementType() == type)
                .mapToInt(StockMovement::getQuantity)
                .sum();
    }

    // ---------- helpers ----------

    private Product requireUsableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getActive()) {
            throw new InvalidStockMovementException("The product is inactive");
        }
        return product;
    }

    private void requireValidDate(LocalDate date) {
        if (date == null) {
            throw new InvalidStockMovementException("The journal date is required");
        }
        if (date.isAfter(LocalDate.now(clock))) {
            throw new InvalidStockMovementException("The journal date cannot be in the future");
        }
    }

    private String optionalNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.length() > NOTE_MAX_LENGTH) {
            throw new InvalidStockMovementException("The note cannot exceed " + NOTE_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    private StockMovement movement(Long productId, LocalDate date, StockLocation location,
                                   StockMovementType type, int quantity, String note) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setJournalDate(date);
        movement.setLocation(location);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setNote(note);
        movement.setCreatedAt(LocalDateTime.now(clock));
        return movement;
    }

    private record StockKey(Long productId, StockLocation location) {
    }
}