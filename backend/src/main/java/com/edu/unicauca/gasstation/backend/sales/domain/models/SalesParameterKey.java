package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.util.Arrays;

/** Parameters owned by Ventas, each with an effective date. The slug is the one used in the URL. */
public enum SalesParameterKey {
    /** Daily contribution to the savings fund, in COP. */
    DAILY_SAVINGS("daily-savings"),
    /** Credibanco commission as a fraction (0.0265 = 2,65 %). */
    CREDIBANCO_COMMISSION_RATE("credibanco-commission-rate"),
    /** Freight per gallon of fuel, in COP. May be zero. */
    FREIGHT_PER_GALLON("freight-per-gallon"),
    /** Financial cost per gallon of fuel, in COP. May be zero. */
    FINANCIAL_COST_PER_GALLON("financial-cost-per-gallon");

    private final String slug;

    SalesParameterKey(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static SalesParameterKey fromSlug(String slug) {
        return Arrays.stream(values()).filter(k -> k.slug.equalsIgnoreCase(slug)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Parámetro desconocido: '" + slug + "'. Valores: "
                        + Arrays.stream(values()).map(SalesParameterKey::slug).toList()));
    }

    public boolean allowsZero() {
        return this == FREIGHT_PER_GALLON || this == FINANCIAL_COST_PER_GALLON;
    }

    /** Whether the sign of the value is acceptable: positive, or also zero for the keys that allow it. */
    public boolean acceptsSignOf(BigDecimal value) {
        return value != null && (allowsZero() ? value.signum() >= 0 : value.signum() > 0);
    }

    public String signMessage() {
        return allowsZero() ? "El valor no puede ser negativo" : "El valor debe ser mayor que cero";
    }

    /** Validates a value for this parameter. */
    public void validate(BigDecimal value) {
        if (!acceptsSignOf(value)) {
            throw new IllegalArgumentException(signMessage());
        }
        if (this == CREDIBANCO_COMMISSION_RATE && value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La comisión se guarda como fracción entre 0 y 1 (2,65 % = 0.0265)");
        }
        if (value.stripTrailingZeros().scale() > 6) {
            throw new IllegalArgumentException("El valor admite máximo 6 decimales");
        }
    }
}
