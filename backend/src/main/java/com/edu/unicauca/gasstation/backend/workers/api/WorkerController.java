package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.ChangeWorkerStatusRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.CreateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.UpdateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.domain.services.WorkerService;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.mappers.WorkerMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Worker management endpoints. No business logic here: validates the body, converts it with
 * {@link WorkerMapper} and delegates to {@link WorkerService}.
 * There is no DELETE on purpose: workers are deactivated, never deleted.
 */
@Tag(name = "Workers", description = "Management of station sellers")
@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final WorkerMapper workerMapper;

    @Operation(summary = "Create a worker", description = "The new worker starts active. 409 if the document is taken, 400 if the role does not exist.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkerResponse create(@Valid @RequestBody CreateWorkerRequest request) {
        return workerMapper.toResponse(workerService.create(workerMapper.toDomain(request)));
    }

    @Operation(summary = "Edit a worker", description = "Replaces name, document and role. Inactive workers can be edited too.")
    @PutMapping("/{id}")
    public WorkerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWorkerRequest request) {
        return workerMapper.toResponse(workerService.update(id, workerMapper.toDomain(request)));
    }

    @Operation(summary = "Activate or deactivate a worker")
    @PatchMapping("/{id}/status")
    public WorkerResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeWorkerStatusRequest request) {
        return workerMapper.toResponse(workerService.changeStatus(id, request.active()));
    }

    @Operation(summary = "Get a worker by id")
    @GetMapping("/{id}")
    public WorkerResponse getById(@PathVariable UUID id) {
        return workerMapper.toResponse(workerService.getById(id));
    }

    @Operation(summary = "List workers", description = "Optional filters: status, role, and text contained in the name or the document.")
    @GetMapping
    public List<WorkerResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) String search) {
        return workerService.list(new WorkerFilter(active, roleId, search)).stream()
                .map(workerMapper::toResponse)
                .toList();
    }
}
