package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.ChangeWorkerStatusRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.CreateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.UpdateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.domain.services.WorkerService;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.mappers.WorkerMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Workers", description = "Management of station sellers: create, edit, activate or deactivate, "
        + "get one and list with filters. Workers are never deleted, only deactivated.")
@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final WorkerMapper workerMapper;

    @Operation(summary = "Create a worker",
            description = "The new worker always starts active; the status is not sent. Name and document are "
                    + "stored without leading or trailing spaces. The document must not belong to another worker "
                    + "and the role must exist.")
    @ApiResponse(responseCode = "201", description = "Worker created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WorkerResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body or the role does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The document already belongs to another worker",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkerResponse create(@Valid @RequestBody CreateWorkerRequest request) {
        return workerMapper.toResponse(workerService.create(workerMapper.toDomain(request)));
    }

    @Operation(summary = "Edit a worker",
            description = "Replaces name, document and role. Inactive workers can be edited too (for example, to "
                    + "fix a typo). The status is changed with PATCH /api/workers/{id}/status.")
    @ApiResponse(responseCode = "400", description = "Invalid body, invalid id or the role does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Worker not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The document already belongs to another worker",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public WorkerResponse update(
            @Parameter(description = "Worker identifier") @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkerRequest request) {
        return workerMapper.toResponse(workerService.update(id, workerMapper.toDomain(request)));
    }

    @Operation(summary = "Activate or deactivate a worker",
            description = "Separate from editing. Setting the status the worker already has is not an error. "
                    + "An inactive worker is kept with its history; it is not available for shift assignment.")
    @ApiResponse(responseCode = "400", description = "Invalid body or invalid id",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Worker not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/status")
    public WorkerResponse changeStatus(
            @Parameter(description = "Worker identifier") @PathVariable UUID id,
            @Valid @RequestBody ChangeWorkerStatusRequest request) {
        return workerMapper.toResponse(workerService.changeStatus(id, request.active()));
    }

    @Operation(summary = "Get a worker by id", description = "Returns the worker whether it is active or not.")
    @ApiResponse(responseCode = "404", description = "Worker not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public WorkerResponse getById(@Parameter(description = "Worker identifier") @PathVariable UUID id) {
        return workerMapper.toResponse(workerService.getById(id));
    }

    @Operation(summary = "List workers",
            description = "Ordered by full name. Every filter is optional and they can be combined.")
    @GetMapping
    public List<WorkerResponse> list(
            @Parameter(description = "Only workers with this status; omit for all")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Only workers with this role; omit for all")
            @RequestParam(required = false) UUID roleId,
            @Parameter(description = "Text contained in the full name or in the document, ignoring case; omit for all")
            @RequestParam(required = false) String search) {
        return workerService.list(new WorkerFilter(active, roleId, search)).stream()
                .map(workerMapper::toResponse)
                .toList();
    }
}
