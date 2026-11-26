package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Rules of the monthly variation alerts that apply from a date on. */
@Getter
@Setter
@NoArgsConstructor
public class VariationAlertSetting {

    private Long id;
    private LocalDate validFrom;
    /** Highest absolute cumulative variation, as a percentage of the cumulative sales, that raises no alert. */
    private BigDecimal cumulativeLimitPercentage;
    /** Consecutive calculated days in a level needed to move to the next one. */
    private Integer escalationDays;
    private Integer shortageLimit1To49;
    private Integer shortageLimit50To74;
    private Integer shortageLimit75To124;
    private Integer shortageLimit125To249;
    private Integer shortageLimit250To499;
    private Integer shortageLimit500OrMore;
    private LocalDateTime createdAt;

    /** Occurrences allowed in the month for shortages of the range. */
    public int shortageLimitOf(GallonRange range) {
        return switch (range) {
            case FROM_1_TO_49 -> shortageLimit1To49;
            case FROM_50_TO_74 -> shortageLimit50To74;
            case FROM_75_TO_124 -> shortageLimit75To124;
            case FROM_125_TO_249 -> shortageLimit125To249;
            case FROM_250_TO_499 -> shortageLimit250To499;
            case FROM_500 -> shortageLimit500OrMore;
        };
    }
}
