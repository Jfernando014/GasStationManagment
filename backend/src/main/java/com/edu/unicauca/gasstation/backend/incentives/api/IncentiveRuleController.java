package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.AdditiveIncentiveResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.CreateRuleRequest;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.CreateRuleResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.IncentiveRuleDtos.RuleHistoryResponse;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.CreateRuleResult;
import com.edu.unicauca.gasstation.backend.incentives.domain.services.IncentiveRuleService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Incentive rule endpoints: reading needs a session, creating needs ADMIN. Rules are not edited or deleted. */
@RestController
@RequestMapping("/api/v1/incentives/rules")
@RequiredArgsConstructor
public class IncentiveRuleController implements IncentiveRuleApi {

    private final IncentiveRuleService service;

    @Override
    @GetMapping
    public List<AdditiveIncentiveResponse> list(
            @Parameter(description = "Fecha a consultar (yyyy-MM-dd); por defecto hoy en Bogotá", example = "2026-02-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.listByDate(date).stream().map(AdditiveIncentiveResponse::from).toList();
    }

    @Override
    @GetMapping("/{productId}/history")
    public List<RuleHistoryResponse> history(
            @Parameter(description = "Id del aditivo", example = "9001") @PathVariable Long productId) {
        return service.history(productId).stream().map(RuleHistoryResponse::from).toList();
    }

    @Override
    @PostMapping
    public ResponseEntity<CreateRuleResponse> create(@Valid @RequestBody CreateRuleRequest request,
                                                     Authentication authentication) {
        IncentiveAdminAuthorization.requireAdmin(authentication);
        CreateRuleResult result = service.create(request.productId(), request.valuePerUnit(), request.validFrom(),
                request.reason());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CreateRuleResponse.from(result));
    }
}
