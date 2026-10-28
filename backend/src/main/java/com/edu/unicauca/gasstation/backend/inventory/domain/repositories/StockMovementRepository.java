package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockBalance;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;

import java.time.LocalDate;
import java.util.List;

public interface StockMovementRepository {

    List<StockMovement> saveAll(List<StockMovement> movements);

    /** The movements of a journal date, in the order they were registered. */
    List<StockMovement> findByJournalDate(LocalDate journalDate);

    /** Units of a product in a location counting every movement up to the date, inclusive. */
    int sumQuantityUpTo(Long productId, StockLocation location, LocalDate date);

    /** Units per product and location counting only the movements before the date (opening stock). */
    List<StockBalance> findBalancesBefore(LocalDate date);

    boolean existsInitial(Long productId, StockLocation location);

    /** Replaces all the SALE movements of the date with the given ones. */
    void replaceSales(LocalDate journalDate, List<StockMovement> sales);
}