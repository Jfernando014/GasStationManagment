package com.edu.unicauca.gasstation.backend.shifts.api;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.AssignRangeRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDtos.AssignShiftRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.GenerateRotationRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.RotationConfirmRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.RotationPreviewRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.RotationPreviewResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.RotationResultResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDtos.ScheduleResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDtos.ScheduleUpdateResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftRotationService;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftScheduleService;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers.ShiftRotationMapper;
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
    private final ShiftRotationService shiftRotationService;
    private final ShiftScheduleMapper shiftScheduleMapper;
    private final ShiftRotationMapper shiftRotationMapper;

    @Operation(summary = "Preview the automatic rotation of a month",
            description = "Calculates, without saving anything, the 2x2 rotation of every active titular for the month: "
                    + "the current one from tomorrow, or a future one from day 1. The phase of each titular is deduced "
                    + "from the grid of the previous days, or suggested when it cannot be deduced. The day titular "
                    + "follows the pairing with the code the support worker already has. Days set by hand are kept "
                    + "unless replaceManual is true, and dates with novelties of the day staff are left untouched. "
                    + "Returns gridVersion, which the confirmation must send back. A month in course without days left "
                    + "answers 200 with zero days and a warning.")
    @ApiResponse(responseCode = "200", description = "Preview calculated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RotationPreviewResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body, past month or phase outside 0 to 5",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A code the rotation needs does not exist or is inactive",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/rotation/preview")
    public RotationPreviewResponse previewRotation(@Valid @RequestBody RotationPreviewRequest request) {
        return shiftRotationMapper.toResponse(
                shiftRotationService.preview(request.month(), request.phases(), request.replaceManual()));
    }

    @Operation(summary = "Confirm the automatic rotation of a month",
            description = "Recalculates the month with the same choices as the preview and saves it in a single "
                    + "transaction, only if the grid did not change since the preview (gridVersion). Written days are "
                    + "saved as generated.")
    @ApiResponse(responseCode = "200", description = "Rotation saved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RotationResultResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body, past month or phase outside 0 to 5",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The grid changed since the preview, a titular needs its phase, "
            + "or a code the rotation needs is inactive",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/rotation/confirm")
    public RotationResultResponse confirmRotation(@Valid @RequestBody RotationConfirmRequest request) {
        return shiftRotationMapper.toResponse(shiftRotationService.confirm(request.month(), request.phases(),
                request.replaceManual(), request.gridVersion()));
    }

    @Operation(summary = "Generate the 2x2 rotation of one titular",
            description = "Same rules as the monthly rotation for a single titular: from tomorrow on, between one full "
                    + "cycle (6 days) and a quarter (startDate + 3 months - 1 day). The phase is deduced from the grid "
                    + "when omitted. Days set by hand and dates with novelties of the day staff are kept, the day "
                    + "titular follows the pairing with the support worker, and written days are saved as generated. "
                    + "Returns the days left without coverage as a warning.")
    @ApiResponse(responseCode = "200", description = "Rotation saved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScheduleUpdateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body, start before tomorrow, end date before start "
            + "date, range shorter than one cycle or longer than a quarter, or phase outside 0 to 5",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Worker not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Worker inactive or not TITULAR, phase required, or a code the "
            + "rotation needs is inactive",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/rotation")
    public ScheduleUpdateResponse generateRotation(@Valid @RequestBody GenerateRotationRequest request) {
        return shiftScheduleMapper.toResponse(shiftRotationService.generateForWorker(
                request.workerId(), request.startDate(), request.endDate(), request.phase()));
    }

    @Operation(summary = "Assign a code to a worker in a range of days",
            description = "Saves the same code on every day of the range, replacing what each day had, with the "
                    + "optional note. With leaveSundaysAsRest the Sundays get DESCANSO. Every day is saved as manual, "
                    + "so the automatic rotation keeps it. From today on, at most 62 days, active workers and codes of "
                    + "their role (DESCANSO is valid for every role).")
    @ApiResponse(responseCode = "200", description = "Days saved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScheduleUpdateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid body, past start date, end date before start date or "
            + "range longer than 62 days",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Worker or active shift code not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Worker inactive or shift code of another role",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/assignments/range")
    public ScheduleUpdateResponse assignRange(@Valid @RequestBody AssignRangeRequest request) {
        return shiftScheduleMapper.toResponse(shiftScheduleService.assignRange(request.workerId(), request.from(),
                request.to(), request.shiftCode(), request.note(), request.leaveSundaysAsRest()));
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
