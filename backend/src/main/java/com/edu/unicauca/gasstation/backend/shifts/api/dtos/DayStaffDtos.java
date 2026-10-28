package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDtos.ShiftSchedule;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.NoveltyType;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.Step1Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Requests and response of step 1 of the journal, the staff of the day.
 */
public final class DayStaffDtos {

    private DayStaffDtos() {
    }

    /**
     * Body of {@code POST /api/shifts/day-staff/{date}/novelties}, and each pending novelty of the confirmation.
     *
     * @param type                REPLACEMENT, ABSENCE, SHIFT_CHANGE or SWAP
     * @param workerId            absent worker, worker whose code changes or first worker of the swap
     * @param counterpartWorkerId replacement (REPLACEMENT) or second worker (SWAP); omitted otherwise
     * @param shiftCode           code to work (SHIFT_CHANGE, optional in REPLACEMENT)
     * @param exceptionalCoverage accepts a code of another role
     * @param reason              3 to 255 characters
     */
    @Schema(description = "Novelty of the day staff. The grid is not modified: the original and the final code are kept.")
    public record RegisterNoveltyRequest(
            @Schema(description = "REPLACEMENT (workerId is absent and counterpartWorkerId covers), ABSENCE (nobody "
                    + "covers), SHIFT_CHANGE (workerId works shiftCode; also adds a resting worker) or SWAP (workerId and "
                    + "counterpartWorkerId swap their codes)", example = "REPLACEMENT")
            @NotNull(message = "El tipo de novedad es obligatorio")
            NoveltyType type,

            @Schema(description = "Absent worker (REPLACEMENT, ABSENCE), worker whose code changes (SHIFT_CHANGE) or "
                    + "first worker of the swap", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
            @NotNull(message = "El vendedor es obligatorio")
            UUID workerId,

            @Schema(description = "Replacement (REPLACEMENT) or second worker of the swap (SWAP); omit it otherwise",
                    example = "0f8c1b8e-31a3-4c42-9a51-3e2f63a4f5b1")
            UUID counterpartWorkerId,

            @Schema(description = "Code to work. Required in SHIFT_CHANGE; in REPLACEMENT the code of the absent worker "
                    + "is used when omitted; ignored in ABSENCE and SWAP", example = "NOCHE")
            String shiftCode,

            @Schema(description = "Accepts a code of a role different from the worker's role", example = "false")
            boolean exceptionalCoverage,

            @Schema(description = "Reason, 3 to 255 characters without leading and trailing spaces",
                    example = "Incapacidad médica")
            String reason) {
    }

    /**
     * Optional body of {@code POST /api/shifts/day-staff/{date}/confirmation}.
     *
     * @param novelties novelties still pending, saved in the same transaction as the confirmation; may be omitted
     */
    @Schema(description = "Novelties to save together with the confirmation. If marking step 1 fails, none is saved.")
    public record ConfirmDayStaffRequest(
            @Schema(description = "Novelties not registered yet; omit or leave empty when they were registered before")
            List<@Valid RegisterNoveltyRequest> novelties) {
    }

    /**
     * Step 1 of a journal: who works, who rests, who is absent, the active novelties and the warnings.
     */
    @Schema(description = "Step 1 of the journal (Personal del día)")
    public record DayStaffStepResponse(
            @Schema(description = "Operational day of the journal", example = "2026-08-05")
            LocalDate date,

            @Schema(description = "State of step 1 in the journal", example = "PENDING")
            Step1Status step1Status,

            @Schema(description = "False when the journal does not exist or is closed: the step is read only")
            boolean editable,

            @Schema(description = "Who works, titulares first and then by start hour")
            List<Member> members,

            @Schema(description = "Workers on DESCANSO")
            List<Resting> resting,

            @Schema(description = "Workers taken out of the day by a novelty")
            List<Absentee> absentees,

            @Schema(description = "Active novelties of the day, in registration order")
            List<Novelty> novelties,

            @Schema(description = "Warnings; they never block the confirmation")
            List<Warning> warnings) {

        /** One worker who works the day. */
        @Schema(description = "Worker who works the day")
        public record Member(
                UUID workerId,
                String fullName,
                @Schema(description = "Code actually worked", example = "NOCHE") String workedCode,
                @Schema(description = "Code in the grid; equals workedCode without novelty", example = "DESCANSO") String gridCode,
                List<ShiftSchedule.Segment> segments,
                @Schema(example = "12") int totalHours,
                @Schema(description = "Role of the day, from the worked code", example = "TITULAR") String dayRole,
                @Schema(description = "1 for titular, 3 for apoyo", example = "1") int dispenser,
                boolean support,
                @Schema(description = "NONE when the worker has no novelty", example = "REPLACEMENT") NoveltyType noveltyType,
                @Schema(description = "Replaced worker (REPLACEMENT) or other worker (SWAP)") UUID otherWorkerId,
                String otherWorkerName,
                String noveltyReason) {
        }

        /** One worker on DESCANSO. */
        public record Resting(UUID workerId, String fullName) {
        }

        /** One worker who does not work because of a novelty. */
        public record Absentee(UUID workerId, String fullName, String gridCode, NoveltyType type, String reason) {
        }

        /** One active novelty; its id is used to cancel it. */
        public record Novelty(
                UUID id,
                NoveltyType type,
                UUID workerId,
                UUID counterpartWorkerId,
                @Schema(description = "Grid code of workerId", example = "NOCHE") String originalShiftCode,
                @Schema(description = "Code actually worked; null in ABSENCE", example = "NOCHE") String finalShiftCode,
                boolean exceptionalCoverage,
                String reason,
                Instant createdAt) {
        }

        /** One warning with a stable code and a message in Spanish. */
        public record Warning(
                @Schema(example = "PAIRING_INCOMPLETE") String code,
                @Schema(example = "Acople incompleto: Camilo Rojas en DIA6 sin su apoyo acoplado") String message) {
        }
    }
}
