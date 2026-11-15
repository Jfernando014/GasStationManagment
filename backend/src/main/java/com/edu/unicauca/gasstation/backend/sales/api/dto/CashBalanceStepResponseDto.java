package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService.Evaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cash balance table of the day and whether step 4 can be completed")
public record CashBalanceStepResponseDto(
        CashBalanceTableInfo table,
        @Schema(description = "True when the difference against Dominus could be calculated")
        boolean canComplete,
        @Schema(description = "What is still missing to complete the step")
        List<String> missing,
        @Schema(description = "CUADRA, PLANILLA_MAYOR or PLANILLA_MENOR; null while the difference is unavailable",
                nullable = true)
        String differenceStatus) {

    public static CashBalanceStepResponseDto from(Evaluation evaluation) {
        return new CashBalanceStepResponseDto(evaluation.table(), evaluation.canComplete(), evaluation.missing(),
                evaluation.differenceStatus() == null ? null : evaluation.differenceStatus().name());
    }
}
