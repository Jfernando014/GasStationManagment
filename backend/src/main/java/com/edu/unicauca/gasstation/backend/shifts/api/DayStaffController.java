package com.edu.unicauca.gasstation.backend.shifts.api;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayStaffDtos.ConfirmDayStaffRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayStaffDtos.DayStaffStepResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayStaffDtos.RegisterNoveltyRequest;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftDayStaffService;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers.DayStaffStepMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints of step 1 of the journal, "Personal del día". No business logic here: validates the input,
 * delegates to {@link ShiftDayStaffService} and converts its results with {@link DayStaffStepMapper}. Every operation
 * answers with step 1 as it is after the change.
 */
@Tag(name = "Day staff (step 1)", description = "Staff of a journal day: the grid with its novelties (replacement, "
        + "absence, shift change, swap), cancellation and confirmation. The grid is never modified. Past dates are "
        + "accepted while the journal of that date is editable.")
@RestController
@RequestMapping("/api/shifts/day-staff")
@RequiredArgsConstructor
public class DayStaffController {

    private final ShiftDayStaffService shiftDayStaffService;
    private final DayStaffStepMapper dayStaffStepMapper;

    @Operation(summary = "Get step 1 of a date",
            description = "Grid of the date with its active novelties applied, workers on DESCANSO apart, absentees "
                    + "and warnings (missing titular, incomplete pairing, uncovered hours). Read only when the "
                    + "journal is closed (editable = false).")
    @ApiResponse(responseCode = "200", description = "Step 1 found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DayStaffStepResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid date",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{date}")
    public DayStaffStepResponse getStep(
            @Parameter(description = "Operational day of the journal (yyyy-MM-dd)", example = "2026-08-05")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dayStaffStepMapper.toResponse(shiftDayStaffService.getStep(date));
    }

    @Operation(summary = "Register a novelty of the day",
            description = "REPLACEMENT, ABSENCE, SHIFT_CHANGE or SWAP with a reason of 3 to 255 characters. A worker "
                    + "takes part in at most one active novelty per day. A code of another role needs "
                    + "exceptionalCoverage = true.")
    @ApiResponse(responseCode = "201", description = "Novelty saved; returns step 1 with it applied",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DayStaffStepResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body, reason, same worker or novelty that does not fit "
            + "the day", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Journal, worker or active shift code not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Journal closed, worker inactive or already working, hours "
            + "crossing, exceptional coverage not marked or another novelty of the worker",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{date}/novelties")
    @ResponseStatus(HttpStatus.CREATED)
    public DayStaffStepResponse registerNovelty(
            @Parameter(description = "Operational day of the journal (yyyy-MM-dd)", example = "2026-08-05")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody RegisterNoveltyRequest request) {
        return dayStaffStepMapper.toResponse(shiftDayStaffService.registerNovelty(date, dayStaffStepMapper.toDraft(request)));
    }

    @Operation(summary = "Cancel a novelty",
            description = "The novelty is kept as cancelled and no longer applies: the day goes back to the grid code.")
    @ApiResponse(responseCode = "200", description = "Novelty cancelled; returns step 1",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DayStaffStepResponse.class)))
    @ApiResponse(responseCode = "404", description = "Journal or novelty not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Journal closed",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{date}/novelties/{noveltyId}")
    public DayStaffStepResponse cancelNovelty(
            @Parameter(description = "Operational day of the journal (yyyy-MM-dd)", example = "2026-08-05")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Novelty to cancel") @PathVariable UUID noveltyId) {
        return dayStaffStepMapper.toResponse(shiftDayStaffService.cancelNovelty(date, noveltyId));
    }

    @Operation(summary = "Confirm the staff of the day",
            description = "Saves the pending novelties of the body (optional) and marks step 1 of the journal as "
                    + "completed, in a single transaction: if marking fails, nothing is saved. Warnings do not block; "
                    + "at least one worker must be left working.")
    @ApiResponse(responseCode = "200", description = "Staff confirmed; returns step 1",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DayStaffStepResponse.class)))
    @ApiResponse(responseCode = "404", description = "Journal not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Journal closed or nobody left working",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{date}/confirmation")
    public DayStaffStepResponse confirm(
            @Parameter(description = "Operational day of the journal (yyyy-MM-dd)", example = "2026-08-05")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody(required = false) ConfirmDayStaffRequest request) {
        return dayStaffStepMapper.toResponse(shiftDayStaffService.confirm(date,
                dayStaffStepMapper.toDrafts(request == null ? null : request.novelties())));
    }
}
