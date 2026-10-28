package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Units of a product in one location on a journal date")
public class LocationStockResponse {

    @Schema(description = "Units before the movements of the day")
    private int opening;
    private int initial;
    private int entries;

    @Schema(description = "Net units transferred: negative in the office, positive in the counter")
    private int transfers;

    private int adjustments;

    @Schema(description = "Units sold in step 2 (positive number)")
    private int sold;

    @Schema(description = "opening + initial + entries + transfers + adjustments - sold")
    private int closing;
}