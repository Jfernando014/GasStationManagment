package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import com.edu.unicauca.gasstation.backend.shifts.NoveltyType;
import com.edu.unicauca.gasstation.backend.shifts.Step1Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
