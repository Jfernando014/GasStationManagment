package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import static com.edu.unicauca.gasstation.backend.shifts.domain.services.ScheduleCoverageCalculator.TITULAR;

import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayLineup;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a {@link DayLineup} into the public records of the day staff. Shared by {@link DayStaffQueryService} and
 * {@link ShiftDayStaffService}, so the step 1 and the query other modules read always show the same people, codes,
 * roles and order. Dispenser, support flag, segments and hours always come from the worked code.
 */
final class DayStaffAssembler {

    /** Titulares before apoyos, then start hour of the first segment, then full name, then worker id. */
    static final Comparator<DayStaff.Member> MEMBER_ORDER = Comparator
            .comparing((DayStaff.Member member) -> !TITULAR.equals(member.dayRole().name()))
            .thenComparingInt(DayStaffAssembler::firstStartHour)
            .thenComparing(DayStaff.Member::fullName)
            .thenComparing(DayStaff.Member::workerId);

    private DayStaffAssembler() {
    }

    /**
     * Working members of the lineup in {@link #MEMBER_ORDER}.
     *
     * @param workers active workers by id; workers missing from it (inactive or unknown) are left out
     */
    static List<DayStaff.Member> members(DayLineup lineup, Map<UUID, ScheduleWorkerInfo> workers) {
        return lineup.working().stream()
                .filter(line -> workers.containsKey(line.workerId()))
                .map(line -> member(line, workers))
                .sorted(MEMBER_ORDER)
                .toList();
    }

    /**
     * Workers taken out of the day by a novelty, ordered by full name.
     *
     * @param workers active workers by id; workers missing from it are left out
     */
    static List<DayStaff.Absentee> absentees(DayLineup lineup, Map<UUID, ScheduleWorkerInfo> workers) {
        return lineup.absences().stream()
                .filter(absence -> workers.containsKey(absence.workerId()))
                .map(absence -> new DayStaff.Absentee(absence.workerId(), workers.get(absence.workerId()).fullName(),
                        absence.gridCode().getCode(), absence.novelty().getType(), absence.novelty().getReason()))
                .sorted(Comparator.comparing(DayStaff.Absentee::fullName).thenComparing(DayStaff.Absentee::workerId))
                .toList();
    }

    private static DayStaff.Member member(DayLineup.Line line, Map<UUID, ScheduleWorkerInfo> workers) {
        ShiftCode worked = line.workedCode();
        ScheduleWorkerInfo worker = workers.get(line.workerId());
        return DayStaff.Member.of(
                worker.id(),
                worker.fullName(),
                worked.getCode(),
                line.gridCodeName(),
                ShiftInfoMapper.toSegments(worked),
                worked.totalHours(),
                ShiftInfoMapper.toRoleInfo(worked.getRole()),
                novelty(line, workers));
    }

    private static DayStaff.Novelty novelty(DayLineup.Line line, Map<UUID, ScheduleWorkerInfo> workers) {
        ShiftNovelty novelty = line.novelty();
        if (novelty == null) {
            return DayStaff.Novelty.none();
        }
        UUID other = line.otherWorkerId();
        ScheduleWorkerInfo otherWorker = other == null ? null : workers.get(other);
        return new DayStaff.Novelty(novelty.getType(), other, otherWorker == null ? null : otherWorker.fullName(),
                novelty.getReason());
    }

    private static int firstStartHour(DayStaff.Member member) {
        return member.segments().isEmpty() ? Integer.MAX_VALUE : member.segments().get(0).startHour();
    }
}
