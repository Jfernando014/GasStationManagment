package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.RotationPreview.RowStatus;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftOrigin;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calculates the rows of the 2x2 rotation of the titulares in a range and decides, for each one, whether it is
 * written or kept. Pure calculation, without database access: the same input always gives the same plan, which is
 * what lets the confirmation save exactly what the preview showed.
 *
 * <p>Each titular follows DIA, DIA, NOCHE, NOCHE, DESCANSO, DESCANSO from its phase, counting elapsed days (so months
 * of any length work and Sundays or holidays do not matter). On day positions the titular code follows what the
 * support worker already has in the grid, using the pairings of the catalog: the support code paired with 9-6 that
 * day puts the titular in 9-6; the support code paired with DIA6 on both days of a pair puts the titular in DIA6 on
 * the second day; otherwise DIA. Support codes without a pairing never change the titular code.
 */
final class RotationPlanner {

    private RotationPlanner() {
    }

    /**
     * Codes the rotation writes, and the support codes paired with the titular codes that need support.
     *
     * @param supportPairedWithNineToSix ids of the support codes paired with 9-6
     * @param supportPairedWithDia6      ids of the support codes paired with DIA6
     */
    record Codes(ShiftCode day, ShiftCode night, ShiftCode rest, ShiftCode dia6, ShiftCode nineToSix,
                 Set<UUID> supportPairedWithNineToSix, Set<UUID> supportPairedWithDia6) {

        /** Reads the pairings from the active catalog, so no support code name is written in the code. */
        static Codes of(ShiftCode day, ShiftCode night, ShiftCode rest, ShiftCode dia6, ShiftCode nineToSix,
                        List<ShiftCode> catalog) {
            return new Codes(day, night, rest, dia6, nineToSix,
                    pairedWith(nineToSix, catalog), pairedWith(dia6, catalog));
        }

        private static Set<UUID> pairedWith(ShiftCode titularCode, List<ShiftCode> catalog) {
            return catalog.stream()
                    .filter(code -> titularCode.getId().equals(code.getPairedWithId()))
                    .map(ShiftCode::getId)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * One titular on one day of the range.
     *
     * @param code     code the rotation would write
     * @param existing current row of the grid; null when there is none
     * @param status   whether the row is written or kept
     */
    record Proposal(UUID workerId, LocalDate date, ShiftCode code, ShiftAssignment existing, RowStatus status) {

        /** True when the confirmation writes this row. */
        boolean writes() {
            return status == RowStatus.NEW || status == RowStatus.REGENERATED || status == RowStatus.REPLACED_MANUAL;
        }
    }

    /**
     * @param from           first day to generate
     * @param to             last day to generate (inclusive)
     * @param phases         phase of each titular to generate, keyed by worker id
     * @param grid           current rows by worker and date; must include the day before {@code from} for the
     *                       pairing of a pair that crosses the start
     * @param supportWorkers active support workers, whose codes drive the pairing
     * @param noveltyDates   dates with novelties of the day staff: nothing of those dates is written
     * @param replaceManual  rewrite manual days too
     */
    static List<Proposal> plan(LocalDate from, LocalDate to, Map<UUID, Integer> phases,
                               Map<UUID, Map<LocalDate, ShiftAssignment>> grid, Set<UUID> supportWorkers,
                               Set<LocalDate> noveltyDates, boolean replaceManual, Codes codes) {
        List<Proposal> proposals = new ArrayList<>();
        phases.forEach((workerId, phase) -> {
            Map<LocalDate, ShiftAssignment> rows = grid.getOrDefault(workerId, Map.of());
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                int position = Math.floorMod(phase + (int) ChronoUnit.DAYS.between(from, date),
                        RotationPhaseDeducer.CYCLE);
                ShiftCode code = codeFor(position, date, grid, supportWorkers, codes);
                ShiftAssignment existing = rows.get(date);
                proposals.add(new Proposal(workerId, date, code, existing,
                        status(existing, noveltyDates.contains(date), replaceManual)));
            }
        });
        return proposals;
    }

    private static ShiftCode codeFor(int position, LocalDate date, Map<UUID, Map<LocalDate, ShiftAssignment>> grid,
                                     Set<UUID> supportWorkers, Codes codes) {
        return switch (RotationPhaseDeducer.kindOf(position)) {
            case NIGHT -> codes.night();
            case REST -> codes.rest();
            case DAY -> dayCode(position, date, grid, supportWorkers, codes);
        };
    }

    /** Code of the day titular according to what the support workers have in the grid that day. */
    private static ShiftCode dayCode(int position, LocalDate date, Map<UUID, Map<LocalDate, ShiftAssignment>> grid,
                                     Set<UUID> supportWorkers, Codes codes) {
        if (supportHas(date, codes.supportPairedWithNineToSix(), grid, supportWorkers)) {
            return codes.nineToSix();
        }
        boolean secondDayOfPair = position == 1;
        if (secondDayOfPair && supportHas(date, codes.supportPairedWithDia6(), grid, supportWorkers)
                && supportHas(date.minusDays(1), codes.supportPairedWithDia6(), grid, supportWorkers)) {
            return codes.dia6();
        }
        return codes.day();
    }

    private static boolean supportHas(LocalDate date, Set<UUID> codeIds,
                                      Map<UUID, Map<LocalDate, ShiftAssignment>> grid, Set<UUID> supportWorkers) {
        return supportWorkers.stream()
                .map(worker -> grid.getOrDefault(worker, Map.of()).get(date))
                .filter(Objects::nonNull)
                .anyMatch(row -> codeIds.contains(row.getShiftCode().getId()));
    }

    /** A manual day is never overwritten without permission; a date with novelties is never touched. */
    private static RowStatus status(ShiftAssignment existing, boolean dateHasNovelties, boolean replaceManual) {
        if (dateHasNovelties) {
            return RowStatus.SKIPPED_NOVELTY;
        }
        if (existing == null) {
            return RowStatus.NEW;
        }
        if (existing.getOrigin() == ShiftOrigin.GENERATED) {
            return RowStatus.REGENERATED;
        }
        return replaceManual ? RowStatus.REPLACED_MANUAL : RowStatus.KEPT_MANUAL;
    }
}
