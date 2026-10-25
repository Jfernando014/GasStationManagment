package com.edu.unicauca.gasstation.backend.shifts;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One worker who works the day, as returned in {@link DayStaff#members()}.
 *
 * <p>{@code dispenser} and {@code support} are kept for the convenience of the consumers, but they are always derived
 * from {@code dayRole}: build a member with {@link #of} so they are filled in for you. The canonical constructor
 * rejects a member whose values contradict {@code dayRole}, and keeps an unmodifiable copy of the segments.
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
public record DayStaffMember(
        UUID workerId,
        String fullName,
        String workedCode,
        String gridCode,
        List<ShiftCodeInfo.Segment> segments,
        int totalHours,
        RoleInfo dayRole,
        int dispenser,
        boolean support,
        DayStaffNovelty novelty) {

    /** Name of the support role in the catalog; every other role works as titular. */
    private static final String SUPPORT_ROLE = "APOYO";

    public DayStaffMember {
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
    public static DayStaffMember of(UUID workerId, String fullName, String workedCode, String gridCode,
                                    List<ShiftCodeInfo.Segment> segments, int totalHours, RoleInfo dayRole,
                                    DayStaffNovelty novelty) {
        Objects.requireNonNull(dayRole, "dayRole");
        return new DayStaffMember(workerId, fullName, workedCode, gridCode, segments, totalHours,
                dayRole, dayRole.dispenser(), isSupport(dayRole), novelty);
    }

    private static boolean isSupport(RoleInfo role) {
        return SUPPORT_ROLE.equals(role.name());
    }
}
