package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayStaffDtos.DayStaffStepResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.DayStaffDtos.RegisterNoveltyRequest;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ScheduleDtos.ShiftSchedule;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayStaffStep;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.NoveltyDraft;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.StaffingWarning;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converts between the step 1 use cases and the DTOs of the REST API. Written by hand: the member is flattened
 * (role as text, novelty fields inline) and the codes of a novelty become text.
 */
@Component
public class DayStaffStepMapper {

    public DayStaffStepResponse toResponse(DayStaffStep step) {
        return new DayStaffStepResponse(
                step.date(),
                step.step1Status(),
                step.editable(),
                step.members().stream().map(DayStaffStepMapper::toMember).toList(),
                step.resting().stream()
                        .map(resting -> new DayStaffStepResponse.Resting(resting.workerId(), resting.fullName()))
                        .toList(),
                step.absentees().stream().map(DayStaffStepMapper::toAbsentee).toList(),
                step.novelties().stream().map(DayStaffStepMapper::toNovelty).toList(),
                step.warnings().stream().map(DayStaffStepMapper::toWarning).toList());
    }

    public NoveltyDraft toDraft(RegisterNoveltyRequest request) {
        return new NoveltyDraft(request.type(), request.workerId(), request.counterpartWorkerId(),
                request.shiftCode() == null || request.shiftCode().isBlank() ? null : request.shiftCode().strip(),
                request.exceptionalCoverage(), request.reason());
    }

    public List<NoveltyDraft> toDrafts(List<RegisterNoveltyRequest> requests) {
        return requests == null ? List.of() : requests.stream().map(this::toDraft).toList();
    }

    private static DayStaffStepResponse.Member toMember(DayStaff.Member member) {
        return new DayStaffStepResponse.Member(
                member.workerId(),
                member.fullName(),
                member.workedCode(),
                member.gridCode(),
                member.segments().stream()
                        .map(segment -> new ShiftSchedule.Segment(segment.startHour(), segment.endHour()))
                        .toList(),
                member.totalHours(),
                member.dayRole().name(),
                member.dispenser(),
                member.support(),
                member.novelty().type(),
                member.novelty().replacedWorkerId(),
                member.novelty().replacedWorkerName(),
                member.novelty().reason());
    }

    private static DayStaffStepResponse.Absentee toAbsentee(DayStaff.Absentee absentee) {
        return new DayStaffStepResponse.Absentee(absentee.workerId(), absentee.fullName(), absentee.gridCode(),
                absentee.type(), absentee.reason());
    }

    private static DayStaffStepResponse.Novelty toNovelty(ShiftNovelty novelty) {
        return new DayStaffStepResponse.Novelty(
                novelty.getId(),
                novelty.getType(),
                novelty.getWorkerId(),
                novelty.getCounterpartWorkerId(),
                novelty.getOriginalShiftCode().getCode(),
                novelty.getFinalShiftCode() == null ? null : novelty.getFinalShiftCode().getCode(),
                novelty.isExceptionalCoverage(),
                novelty.getReason(),
                novelty.getCreatedAt());
    }

    private static DayStaffStepResponse.Warning toWarning(StaffingWarning warning) {
        return new DayStaffStepResponse.Warning(warning.type().name(), warning.message());
    }
}
