package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.*;
import com.edu.unicauca.gasstation.backend.sales.application.SalesDistributionService;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DistributionProfileRepository.StoredProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Tag(name="Sales distribution", description="Explicit parameter snapshots and workbook-based target calculations")
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesDistributionController {
    private final SalesDistributionService service;

    @PostMapping("/distribution-profiles")
    @Operation(summary="Register a parameter snapshot", description="ADMIN only. No workbook constants are loaded as defaults.")
    public ResponseEntity<StoredProfile> register(@Valid @RequestBody CreateDistributionProfileRequest request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        var stored = service.register(request.toDomain());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(stored.id()).toUri()).body(stored);
    }
    @GetMapping("/distribution-profiles/{id}")
    @Operation(summary="Read a parameter snapshot")
    public StoredProfile findById(@PathVariable long id) { return service.findById(id); }

    @PostMapping("/targets/{id}/distribution")
    @Operation(summary="Calculate individual monthly targets", description="ADMIN only. Reuses the stored profile. Explicit assignments accept optional inclusive endDate for repeating the same shift within a month; no rotation is inferred. Assignments are validated against public workers/shift contracts, not fetched from the schedule.")
    public SalesDistributionResponse calculate(@PathVariable long id, @Valid @RequestBody CalculateDistributionRequest request,
                                                    Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        var assignments = request.toDomainAssignments();
        return SalesDistributionResponse.from(id,request.profileId(),service.calculate(id, request.profileId(), request.outputScale(), assignments));
    }
}
