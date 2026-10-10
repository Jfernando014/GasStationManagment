package com.edu.unicauca.gasstation.backend.shifts.api;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.AssignShiftRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.GenerateRotationRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleUpdateResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftScheduleService;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers.ShiftScheduleMapper;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints of the monthly shift schedule. No business logic here: validates the input, delegates to
 * {@link ShiftScheduleService} and converts its results with {@link ShiftScheduleMapper}.
 */
@Tag(name = "Shift schedule", description = "Monthly schedule: which shift code each worker has each day, with the "
        + "daily coverage. Changing a day means replacing its code.")
@RestController
@RequestMapping("/api/shifts/schedule")
@RequiredArgsConstructor
public class ShiftScheduleController {

    private final ShiftScheduleService shiftScheduleService;
    private final ShiftScheduleMapper shiftScheduleMapper;

    @Operation(summary = "Generate the 2x2 rotation of a titular",
            description = "Repeats DIA, DIA, NOCHE, NOCHE, DESCANSO, DESCANSO from startDate to endDate, replacing "
                    + "everything the worker had in that range. Only for active TITULAR workers and from today on. "
                    + "The range covers at least one full cycle (6 days) and at most a quarter from startDate "
                    + "(startDate + 3 months - 1 day); shorter changes are made day by day. The exact day code "
                    + "(DIA6, 9-6) is changed afterwards by editing that day. Returns the days left without coverage "
                    + "as a warning.")
    @ApiResponse(responseCode = "200", description = "Rotation saved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScheduleUpdateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body, past start date, end date before start date, "
            + "or range shorter than one cycle or longer than a quarter",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Worker not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Worker inactive or not TITULAR",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/rotation")
    public ScheduleUpdateResponse generateRotation(@Valid @RequestBody GenerateRotationRequest request) {
        return shiftScheduleMapper.toResponse(
                shiftScheduleService.generateRotation(request.workerId(), request.startDate(), request.endDate()));
    }

    @Operation(summary = "Save or edit the shift of a worker on one day",
            description = "Creates the day or replaces its code and note. The code must be active and belong to the "
                    + "worker's role; DESCANSO is valid for every role. Only from today on and for active workers. "
                    + "Returns the day if it is left without coverage, as a warning.")
    @ApiResponse(responseCode = "200", description = "Day saved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScheduleUpdateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body or past date",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Worker or active shift code not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Worker inactive or shift code of another role",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/assignments")
    public ScheduleUpdateResponse assign(@Valid @RequestBody AssignShiftRequest request) {
        return shiftScheduleMapper.toResponse(
                shiftScheduleService.assign(request.workerId(), request.date(), request.shiftCode(), request.note()));
    }

    @Operation(summary = "Get the schedule of a date range with its coverage",
            description = "Returns what is stored, past days included. Only active workers with at least one shift "
                    + "in the range are listed. The filters only affect the listed rows; the coverage of every day is "
                    + "calculated over every active worker.")
    @ApiResponse(responseCode = "200", description = "Schedule found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScheduleResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or invalid parameter, or 'to' before 'from'",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public ScheduleResponse getSchedule(
            @Parameter(description = "First day of the range (yyyy-MM-dd)", example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day of the range, inclusive (yyyy-MM-dd); not before 'from'",
                    example = "2026-11-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Only this worker; omit for all")
            @RequestParam(required = false) UUID workerId,
            @Parameter(description = "Only codes of this period (DAY or NIGHT); DESCANSO is not listed when "
                    + "filtering; omit for all")
            @RequestParam(required = false) ShiftPeriod period) {
        return shiftScheduleMapper.toResponse(shiftScheduleService.getSchedule(from, to, workerId, period));
    }
}
