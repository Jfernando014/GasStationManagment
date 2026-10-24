package com.edu.unicauca.gasstation.backend.sales;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Integration contract for the owner of journal-close sales. No production adapter exists yet.
 * An available snapshot certifies complete, final sales for the inclusive operational-date range.
 * IDs must be stable and unique per sale line. Amount is the recorded net COP amount, never
 * reconstructed from current catalog prices. Category is the category at the time of sale.
 * Quantity counts sold product units (not baskets, litres or packages converted to another unit).
 * Refunds/adjustments require a separate approved policy; negative lines are not accepted here.
 */
public interface BasketSalesReadPort {
    Snapshot read(LocalDate fromInclusive, LocalDate toInclusive);

    record Snapshot(boolean available, List<Sale> sales) {
        public Snapshot {
            sales = List.copyOf(sales);
            if (!available && !sales.isEmpty()) throw new IllegalArgumentException("Unavailable source cannot supply sales");
        }
    }

    record Sale(String id, LocalDate date, UUID workerId, long productId, ProductCategory category,
                BigDecimal amount, BigDecimal quantity) {
        public Sale {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Sale ID required");
            Objects.requireNonNull(date, "date");
            Objects.requireNonNull(workerId, "workerId");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(quantity, "quantity");
            if (workerId.equals(new UUID(0, 0)) || productId <= 0)
                throw new IllegalArgumentException("Valid worker and product IDs required");
            if (amount.signum() < 0 || quantity.signum() < 0)
                throw new IllegalArgumentException("Sale amount and quantity must be nonnegative");
        }
    }
}
