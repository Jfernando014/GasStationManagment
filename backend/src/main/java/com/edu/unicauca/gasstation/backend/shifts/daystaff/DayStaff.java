package com.edu.unicauca.gasstation.backend.shifts.daystaff;

import com.edu.unicauca.gasstation.backend.shifts.catalog.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Staff of one operational day, returned by {@link DayStaffQuery}. The lists are unmodifiable copies, whoever builds
 * the record. The nested types are the rest of the contract: who works, who is absent, the novelty of a member, and
 * the enums of the day.
 *
 * @param date             operational day
 * @param members          who works, titulares first then apoyos
 * @param absentees        who was in the grid but does not work (novelties)
 * @param step1Status      state of step 1 of the journal of that day
 * @param noveltiesApplied true only when step 1 is CONFIRMED and novelties were applied
 * @param warnings         coverage warnings of the day
 */
public record DayStaff(
        LocalDate date,
        List<Member> members,
        List<Absentee> absentees,
        Step1Status step1Status,
        boolean noveltiesApplied,
        List<Warning> warnings) {

    public DayStaff {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(step1Status, "step1Status");
        members = List.copyOf(members);
        absentees = List.copyOf(absentees);
        warnings = List.copyOf(warnings);
    }

    /**
     * One worker who works the day, as returned in {@link DayStaff#members()}.
     *
     * <p>{@code dispenser} and {@code support} are kept for the convenience of the consumers, but they are always
     * derived from {@code dayRole}: build a member with {@link #of} so they are filled in for you. The canonical
     * constructor rejects a member whose values contradict {@code dayRole}, and keeps an unmodifiable copy of the
     * segments.
     *
     * @param workerId   worker identifier, the same as in the {@code workers} module
     * @param fullName   full name, as registered
     * @param workedCode code actually worked that day
     * @param gridCode   code in the grid (malla); equals workedCode without novelty
     * @param segments   segments of workedCode, same semantics as {@link ShiftCodeInfo}
     * @param totalHours sum of the hours of the segments of workedCode
     * @param dayRole    role of the day, derived from workedCode, not from the worker
     * @param dispenser  {@code dayRole.dispenser()}: TITULAR = 1, APOYO = 3
     * @param support    true when dayRole is APOYO
     * @param novelty    never null; type NONE when there is no novelty
     */
    public record Member(
            UUID workerId,
            String fullName,
            String workedCode,
            String gridCode,
            List<ShiftCodeInfo.Segment> segments,
            int totalHours,
            RoleInfo dayRole,
            int dispenser,
            boolean support,
            Novelty novelty) {

        /** Name of the support role in the catalog; every other role works as titular. */
        private static final String SUPPORT_ROLE = "APOYO";

        public Member {
            Objects.requireNonNull(workerId, "workerId");
            Objects.requireNonNull(workedCode, "workedCode");
            Objects.requireNonNull(gridCode, "gridCode");
            Objects.requireNonNull(dayRole, "dayRole");
            Objects.requireNonNull(novelty, "novelty");
            segments = List.copyOf(segments);
            if (dispenser != dayRole.dispenser()) {
                throw new IllegalArgumentException("dispenser " + dispenser + " does not match the dispenser "
                        + dayRole.dispenser() + " of role " + dayRole.name());
            }
            if (support != isSupport(dayRole)) {
                throw new IllegalArgumentException("support " + support + " does not match role " + dayRole.name());
            }
        }

        /** A member whose dispenser and support flag are derived from {@code dayRole}. */
        public static Member of(UUID workerId, String fullName, String workedCode, String gridCode,
                                List<ShiftCodeInfo.Segment> segments, int totalHours, RoleInfo dayRole,
                                Novelty novelty) {
            Objects.requireNonNull(dayRole, "dayRole");
            return new Member(workerId, fullName, workedCode, gridCode, segments, totalHours,
                    dayRole, dayRole.dispenser(), isSupport(dayRole), novelty);
        }

        private static boolean isSupport(RoleInfo role) {
            return SUPPORT_ROLE.equals(role.name());
        }
    }

    /**
     * Novelty applied to a member of the day.
     *
     * @param type               NONE when there is no novelty
     * @param replacedWorkerId   only REPLACEMENT and SWAP; else null
     * @param replacedWorkerName only REPLACEMENT and SWAP; else null
     * @param reason             null when type is NONE
     */
    public record Novelty(
            NoveltyType type,
            UUID replacedWorkerId,
            String replacedWorkerName,
            String reason) {

        /** A member without novelty. */
        public static Novelty none() {
            return new Novelty(NoveltyType.NONE, null, null, null);
        }
    }

    /**
     * A worker who was in the grid but does not work the day because of a novelty (absence or replacement).
     * A normal DESCANSO is never an absentee.
     *
     * @param workerId identifier of the worker
     * @param fullName full name, as registered
     * @param gridCode code the worker had in the grid
     * @param type     novelty that took the worker out of the day
     * @param reason   reason of the novelty
     */
    public record Absentee(
            UUID workerId,
            String fullName,
            String gridCode,
            NoveltyType type,
            String reason) {
    }

    /** Type of novelty of a day: replacement, absence, shift change or swap, or none. */
    public enum NoveltyType {
        NONE,
        REPLACEMENT,
        ABSENCE,
        SHIFT_CHANGE,
        SWAP
    }

    /**
     * State of step 1 (day staff) of the journal of a date.
     *
     * <p>UNKNOWN was returned only while this module could not ask the sales module for the journal; now it is
     * never returned.
     */
    public enum Step1Status {
        UNKNOWN,
        NO_JOURNAL,
        PENDING,
        CONFIRMED
    }

    /**
     * Coverage warning of a day. NO_SCHEDULE is returned alone, only when the day has no active assignment; the other
     * three follow the same rules as the coverage of the shift schedule.
     */
    public enum Warning {
        NO_SCHEDULE,
        DAY_TITULAR_MISSING,
        NIGHT_TITULAR_MISSING,
        SUPPORT_MISSING
    }
}
