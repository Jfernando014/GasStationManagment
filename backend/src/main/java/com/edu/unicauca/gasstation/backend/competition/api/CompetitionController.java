package com.edu.unicauca.gasstation.backend.competition.api;

import com.edu.unicauca.gasstation.backend.competition.api.dto.CompetitionRequests.*;
import com.edu.unicauca.gasstation.backend.competition.domain.models.*;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.ObservationRepository.History;
import com.edu.unicauca.gasstation.backend.competition.domain.services.*;
import com.edu.unicauca.gasstation.backend.competition.domain.services.MarketPriceService.*;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/competition")
@Tag(name = "Competitor prices", description = "HU-37. All operations require ADMIN. Surveys are append-only.")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CompetitionController {
    private final CompetitionService service;
    private final MarketPriceService market;

    @PostMapping("/competitors")
    @Operation(summary = "Register an active competitor")
    public ResponseEntity<Competitor> register(@Valid @RequestBody CompetitorRequest request, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        var result = service.register(request.name(), request.brand(), request.location());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.id()).toUri()).body(result);
    }
    @GetMapping("/competitors")
    @Operation(summary = "List competitors", description = "Optional active filter. Omitted includes inactive competitors.")
    public List<Competitor> list(@RequestParam(required = false) Boolean active, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.list(active);
    }
    @GetMapping("/competitors/{id}")
    @Operation(summary = "Read a competitor")
    public Competitor find(@PathVariable long id, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.find(id);
    }
    @PutMapping("/competitors/{id}")
    @Operation(summary = "Replace competitor identity information, preserving status and history")
    public Competitor update(@PathVariable long id, @Valid @RequestBody CompetitorRequest request, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.update(id, request.name(), request.brand(), request.location());
    }
    @PatchMapping("/competitors/{id}/status")
    @Operation(summary = "Activate or deactivate a competitor without deleting history")
    public Competitor status(@PathVariable long id, @Valid @RequestBody StatusRequest request, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.changeStatus(id, request.active());
    }
    @PostMapping("/observations")
    @Operation(summary = "Append a manual price observation", description = "observedAt must be supplied with a timezone and cannot be in the future. Inactive competitor returns 409.")
    public ResponseEntity<PriceObservation> observe(@Valid @RequestBody ObservationRequest request, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        var result = service.observe(request.competitorId(), request.fuelType(), request.pricePerGallon(), request.observedAt());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.id()).toUri()).body(result);
    }
    @GetMapping("/observations/{id}")
    @Operation(summary = "Read a historical observation, including inactive competitors")
    public PriceObservation observation(@PathVariable long id, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.observation(id);
    }
    @GetMapping("/observations")
    @Operation(summary = "Page through history", description = "Optional competitorId, fuelType, from and to (inclusive instants). page starts at 0; size 1..200. Includes inactive competitors.")
    public History history(@RequestParam(required = false) Long competitorId, @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.history(competitorId, fuelType, from, to, page, size);
    }
    @GetMapping("/observations/latest")
    @Operation(summary = "Latest observation per competitor and fuel", description = "Optional competitorId, fuelType, asOf and includeInactive (default false). No maximum survey age. Ties resolved by greatest observation ID.")
    public List<PriceObservation> latest(@RequestParam(required = false) Long competitorId, @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Instant asOf, @RequestParam(defaultValue = "false") boolean includeInactive, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return service.latest(competitorId, fuelType, asOf, includeInactive);
    }
    @GetMapping("/comparison")
    @Operation(summary = "Compare Bellavista against the competitor mean and minimum", description = "Optional fuelType (all fuels if omitted) and asOf (now if omitted). Uses currently active competitors; Bellavista validity is daily in America/Bogota. No maximum survey age.")
    public Comparison compare(@RequestParam(required = false) FuelType fuelType, @RequestParam(required = false) Instant asOf, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return market.compare(fuelType, asOf);
    }
    @PostMapping("/simulation")
    @Operation(summary = "Simulate a price without persistence", description = "Competitive percentage = (proposed-reference)/reference*100. Optional explicit acquisitionCost enables gross margin = (proposed-cost)/proposed*100; excludes taxes and operating expenses.")
    public Simulation simulate(@Valid @RequestBody SimulationRequest request, Authentication auth) {
        CompetitionAdminAuthorization.requireAdmin(auth);
        return market.simulate(request.fuelType(), request.proposedPrice(), request.acquisitionCost(), request.asOf());
    }
}
