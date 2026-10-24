package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import com.edu.unicauca.gasstation.backend.shifts.DayStaffAbsentee;
import com.edu.unicauca.gasstation.backend.shifts.DayStaffMember;
import com.edu.unicauca.gasstation.backend.shifts.Step1Status;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Step 1 of a journal: the grid of the day with its active novelties, whether they are confirmed or not.
 *
 * @param date        operational day of the journal
 * @param step1Status state of step 1 in the journal
 * @param editable    false when the journal does not exist or is closed: the step is read only
 * @param members     who works, in the order of {@code DayStaffQuery} (titulares first, then by start hour)
 * @param resting     workers on DESCANSO, shown apart
 * @param absentees   workers taken out of the day by a novelty
 * @param novelties   active novelties of the day, in registration order
 * @param warnings    coverage and pairing warnings; they never block
 */
public record DayStaffStep(
        LocalDate date,
        Step1Status step1Status,
        boolean editable,
        List<DayStaffMember> members,
        List<RestingWorker> resting,
        List<DayStaffAbsentee> absentees,
        List<ShiftNovelty> novelties,
        List<StaffingWarning> warnings) {

    public DayStaffStep {
        members = List.copyOf(members);
        resting = List.copyOf(resting);
        absentees = List.copyOf(absentees);
        novelties = List.copyOf(novelties);
        warnings = List.copyOf(warnings);
    }

    /** A worker on DESCANSO that day. */
    public record RestingWorker(UUID workerId, String fullName) {
    }
}
