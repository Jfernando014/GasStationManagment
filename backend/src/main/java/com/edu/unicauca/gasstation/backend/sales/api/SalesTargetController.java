package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesTargetNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.mappers.SalesTargetApiMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Tag(name = "Sales targets", description = "Registration and queries of configured annual and monthly targets")
@RequestMapping("/api/v1/sales/targets")
@RequiredArgsConstructor
public class SalesTargetController {
    private final SalesTargetService service;
    private final SalesTargetApiMapper mapper;

    @PostMapping
    @Operation(summary = "Register a configured target", description = "Requires the exact ADMIN authority. No automatic annual-to-monthly calculation.")
    public ResponseEntity<SalesTargetResponse> register(@Valid @RequestBody CreateSalesTargetRequest request,
                                                        Authentication authentication) {
        requireAdmin(authentication);
        var stored = service.register(mapper.toDomain(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(stored.id()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(stored));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a configured target by ID")
    public SalesTargetResponse findById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id).orElseThrow(SalesTargetNotFoundException::new));
    }

    @GetMapping("/by-identity")
    @Operation(summary = "Find a target by period and category", description = "Omitting month means annual. An empty month is invalid.")
    public SalesTargetResponse findByIdentity(@RequestParam int year, @RequestParam SalesTargetCategory category,
                                              @RequestParam(required = false) String month) {
        Integer parsedMonth = null;
        if (month != null) {
            if (month.isBlank()) throw new IllegalArgumentException("month no puede estar vacío");
            parsedMonth = Integer.valueOf(month);
        }
        var identity = new SalesTargetIdentity(new SalesTargetPeriod(year, parsedMonth), category);
        return mapper.toResponse(service.findByIdentity(identity).orElseThrow(SalesTargetNotFoundException::new));
    }

    @GetMapping
    @Operation(summary = "List annual and monthly targets of a year")
    public List<SalesTargetResponse> findByYear(@RequestParam int year) {
        return service.findByYear(year).stream().map(mapper::toResponse).toList();
    }

    // The sole HTTP write entry point calls this before mapping or invoking the service.
    private static void requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Se requiere autenticación");
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Se requiere la autoridad ADMIN");
        }
    }
}
