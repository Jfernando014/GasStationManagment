package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.api.AdditiveSalesDtos.AdditiveSalesDayResponse;
import com.edu.unicauca.gasstation.backend.incentives.api.AdditiveSalesDtos.SyncResponse;
import com.edu.unicauca.gasstation.backend.incentives.domain.services.AdditiveSalesService;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Additive sales endpoints: reading needs a session, reprocessing a date needs ADMIN. */
@RestController
@RequestMapping("/api/v1/incentives/additive-sales")
@RequiredArgsConstructor
public class AdditiveSalesController implements AdditiveSalesApi {

    private final AdditiveSalesService service;

    @Override
    @GetMapping("/{date}")
    public AdditiveSalesDayResponse getDay(
            @Parameter(description = "Fecha operativa de la jornada (yyyy-MM-dd)", example = "2026-02-10")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return AdditiveSalesDayResponse.from(service.getDay(date));
    }

    @Override
    @GetMapping("/pending")
    public List<SyncResponse> listPending() {
        return service.listPending().stream().map(SyncResponse::from).toList();
    }

    @Override
    @PostMapping("/{date}/reprocess")
    public AdditiveSalesDayResponse reprocess(
            @Parameter(description = "Fecha operativa de la jornada (yyyy-MM-dd)", example = "2026-02-10")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        IncentiveAdminAuthorization.requireAdmin(authentication);
        return AdditiveSalesDayResponse.from(service.reprocess(date));
    }
}
