package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class VariationAlertSettingResponse {

    private Long id;
    private LocalDate validFrom;
    private BigDecimal cumulativeLimitPercentage;
    private Integer escalationDays;
    private Integer shortageLimit1To49;
    private Integer shortageLimit50To74;
    private Integer shortageLimit75To124;
    private Integer shortageLimit125To249;
    private Integer shortageLimit250To499;
    private Integer shortageLimit500OrMore;
    private LocalDateTime createdAt;
}
