package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.util.Arrays;

/** Parameters owned by Ventas, each with an effective date. The slug is the one used in the URL. */
public enum SalesParameterKey {
    /** Daily contribution to the savings fund, in COP. */
    DAILY_SAVINGS("daily-savings"),
    /** Credibanco commission as a fraction (0.0265 = 2,65 %). */
    CREDIBANCO_COMMISSION_RATE("credibanco-commission-rate");

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

    /** Validates a value for this parameter. */
    public void validate(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("El valor debe ser mayor que cero");
        }
        if (this == CREDIBANCO_COMMISSION_RATE && value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La comisión se guarda como fracción entre 0 y 1 (2,65 % = 0.0265)");
        }
        if (value.stripTrailingZeros().scale() > 6) {
            throw new IllegalArgumentException("El valor admite máximo 6 decimales");
        }
    }
}
