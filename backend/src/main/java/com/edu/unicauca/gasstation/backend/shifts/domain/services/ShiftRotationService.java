package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.RotationPreview.PhaseSource;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.RotationPreview.RowStatus;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.RotationPreview;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftOrigin;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases of the automatic 2x2 rotation: preview a month, confirm it, and generate one titular in a range.
 *
 * <p>The rotation only writes from tomorrow on, never today or the past, so it never touches a day that already has a
 * journal. Days set by hand are kept unless the administrator asks to replace them, and a date with novelties of the
 * day staff is never touched, because those novelties keep the grid code of that day. The preview keeps nothing on
 * the server: the confirmation recalculates the same plan and saves it only if the grid fingerprint did not change.
 */
@Service
@RequiredArgsConstructor
public class ShiftRotationService {

    /** Time zone of the station. "Today" is always calculated with it; the server may run in UTC. */
    private static final ZoneId STATION_ZONE = ZoneId.of("America/Bogota");

    /** Longest rotation of one titular, counted from its start date: bounds the rows written in one request. */
    private static final int MAX_ROTATION_MONTHS = 3;

    /** Days of grid read before the first day to generate: the wide window of the phase deduction. */
    private static final int LOOKBACK_DAYS = 12;

    /** Three titulares with phases two positions apart give every day one day titular, one night and one resting. */
    private static final int EXPECTED_TITULARES = 3;

    private static final String DAY = "DIA";
    private static final String NIGHT = "NOCHE";
    private static final String REST = "DESCANSO";
    private static final String DIA6 = "DIA6";
    private static final String NINE_TO_SIX = "9-6";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final ShiftNoveltyRepository shiftNoveltyRepository;
    private final ScheduleWorkerDirectory scheduleWorkerDirectory;

    /**
     * Calculates the rotation of every active titular for a month without saving anything.
     *
     * @param phases        phases chosen by the administrator, keyed by worker id; they win over the deduced ones
     * @param replaceManual rewrite manual days too
     * @throws ShiftException {@code INVALID_MONTH} if the month already ended
     * @throws ShiftException {@code INVALID_PHASE} if a phase is outside 0 to 5
     * @throws ShiftException {@code ROTATION_CODE_INACTIVE} if a code the rotation needs does not exist or is inactive
     */
    @Transactional(readOnly = true)
    public RotationPreview preview(YearMonth month, Map<UUID, Integer> phases, boolean replaceManual) {
        Range range = monthRange(month);
        Map<UUID, Integer> chosen = validPhases(phases);
        if (range.isEmpty()) {
            return emptyPreview(month);
        }
        Context context = load(range.from(), range.to());
        return context.plan(context.titulares(), chosen, replaceManual).toPreview(month);
    }

    /**
     * Saves the rotation of a month exactly as the preview calculated it, in a single transaction.
     *
     * @param gridVersion fingerprint returned by the preview
     * @throws ShiftException {@code SCHEDULE_CHANGED} if the grid changed since the preview; nothing is saved
     * @throws ShiftException {@code PHASE_REQUIRED} if a titular needs a phase the request does not choose
     */
    @Transactional
    public RotationPreview.Outcome confirm(YearMonth month, Map<UUID, Integer> phases, boolean replaceManual,
                                           String gridVersion) {
        Range range = monthRange(month);
        Map<UUID, Integer> chosen = validPhases(phases);
        if (range.isEmpty()) {
            return new RotationPreview.Outcome(0, 0, List.of());
        }
        Context context = load(range.from(), range.to());
        Plan plan = context.plan(context.titulares(), chosen, replaceManual);
        if (!plan.version().equals(gridVersion)) {
            throw new ShiftException(ShiftError.SCHEDULE_CHANGED);
        }
        plan.requirePhases();
        save(plan);
        return new RotationPreview.Outcome(plan.counts().generated(), plan.counts().kept(), plan.uncoveredDates());
    }

    /**
     * Generates the rotation of one titular in a range, with the same rules as the monthly rotation: from tomorrow on,
     * manual days and dates with novelties kept, pairing with the support worker applied, rows saved as generated.
     *
     * @param phase position of {@code startDate} in the cycle; null to deduce it from the grid
     * @throws ShiftException {@code INVALID_RANGE} if {@code startDate} is before tomorrow
     * @throws ShiftException {@code INVALID_DATE_RANGE} if {@code endDate} is before {@code startDate}
     * @throws ShiftException {@code INVALID_ROTATION_LENGTH} if the range is shorter than one cycle or longer than a quarter
     * @throws ShiftException {@code WORKER_NOT_FOUND} if the worker does not exist
     * @throws ShiftException {@code WORKER_INACTIVE} if the worker is inactive
     * @throws ShiftException {@code ROTATION_NOT_ALLOWED} if the worker is not titular
     * @throws ShiftException {@code PHASE_REQUIRED} if the phase is missing and cannot be deduced
     */
    @Transactional
    public ScheduleChange generateForWorker(UUID workerId, LocalDate startDate, LocalDate endDate, Integer phase) {
        LocalDate tomorrow = LocalDate.now(STATION_ZONE).plusDays(1);
        if (startDate.isBefore(tomorrow)) {
            throw new ShiftException(ShiftError.INVALID_RANGE, "La rotación solo puede empezar desde mañana (" + tomorrow + ")");
        }
        if (endDate.isBefore(startDate)) {
            throw new ShiftException(ShiftError.INVALID_DATE_RANGE, startDate, endDate);
        }
        LocalDate minEndDate = startDate.plusDays(RotationPhaseDeducer.CYCLE - 1L);
        LocalDate maxEndDate = startDate.plusMonths(MAX_ROTATION_MONTHS).minusDays(1);
        if (endDate.isBefore(minEndDate) || endDate.isAfter(maxEndDate)) {
            throw new ShiftException(ShiftError.INVALID_ROTATION_LENGTH, startDate, minEndDate, maxEndDate);
        }
        Map<UUID, Integer> chosen = phase == null ? Map.of() : validPhases(Map.of(workerId, phase));
        ScheduleWorkerInfo worker = scheduleWorkerDirectory.getWorkersByIds(Set.of(workerId)).get(workerId);
        if (worker == null) {
            throw new ShiftException(ShiftError.WORKER_NOT_FOUND, workerId);
        }
        if (!worker.active()) {
            throw new ShiftException(ShiftError.WORKER_INACTIVE, workerId);
        }
        Context context = load(startDate, endDate);
        if (!context.isTitular(worker)) {
            throw new ShiftException(ShiftError.ROTATION_NOT_ALLOWED);
        }
        Plan plan = context.plan(List.of(worker), chosen, false);
        plan.requirePhases();
        save(plan);
        return new ScheduleChange(plan.counts().generated(), plan.uncoveredDates());
    }

    /** Writes the rows the plan generates; an existing row keeps its id so the unique (worker, date) holds. */
    private void save(Plan plan) {
        List<ShiftAssignment> rows = new ArrayList<>();
        for (RotationPlanner.Proposal proposal : plan.proposals()) {
            if (!proposal.writes()) {
                continue;
            }
            ShiftAssignment existing = proposal.existing();
            if (existing == null) {
                rows.add(ShiftAssignment.generated(proposal.workerId(), proposal.date(), proposal.code()));
            } else {
                existing.regenerate(proposal.code());
                rows.add(existing);
            }
        }
        shiftAssignmentRepository.saveAll(rows);
    }

    /** First day: the latest of tomorrow and the first day of the month; last day: the end of the month. */
    private static Range monthRange(YearMonth month) {
        Objects.requireNonNull(month, "month");
        LocalDate today = LocalDate.now(STATION_ZONE);
        if (month.isBefore(YearMonth.from(today))) {
            throw new ShiftException(ShiftError.INVALID_MONTH, month);
        }
        LocalDate firstDay = month.atDay(1);
        LocalDate tomorrow = today.plusDays(1);
        return new Range(firstDay.isAfter(tomorrow) ? firstDay : tomorrow, month.atEndOfMonth());
    }

    private static Map<UUID, Integer> validPhases(Map<UUID, Integer> phases) {
        if (phases == null) {
            return Map.of();
        }
        phases.values().forEach(phase -> {
            if (phase == null || phase < 0 || phase >= RotationPhaseDeducer.CYCLE) {
                throw new ShiftException(ShiftError.INVALID_PHASE, phase == null ? -1 : phase);
            }
        });
        return Map.copyOf(phases);
    }

    /** The month in course has no days left from tomorrow on: an answer with zeros and a warning, not an error. */
    private static RotationPreview emptyPreview(YearMonth month) {
        return new RotationPreview(month, null, null, List.of(), List.of(), List.of(), List.of(),
                List.of(new RotationPreview.Warning("NO_DAYS_TO_GENERATE", "No hay días por generar en " + month)),
                new RotationPreview.Counts(0, 0, 0), ScheduleVersion.of(List.of(), List.of(), List.of()));
    }

    /**
     * Reads everything the plan needs in a few queries: the active catalog, the active workers, the grid from twelve
     * days before the range (for the phase and for a pair that crosses the start) and the novelties of the range.
     */
    private Context load(LocalDate from, LocalDate to) {
        Map<String, ShiftCode> catalog = shiftCodeRepository.findActive().stream()
                .collect(Collectors.toMap(ShiftCode::getCode, Function.identity()));
        RotationPlanner.Codes codes = RotationPlanner.Codes.of(required(catalog, DAY), required(catalog, NIGHT),
                required(catalog, REST), required(catalog, DIA6), required(catalog, NINE_TO_SIX),
                List.copyOf(catalog.values()));
        Map<UUID, ScheduleWorkerInfo> active = scheduleWorkerDirectory.getActiveWorkers().stream()
                .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
        List<ShiftAssignment> rows = shiftAssignmentRepository.findByWorkDateBetween(from.minusDays(LOOKBACK_DAYS), to);
        List<ShiftNovelty> novelties = shiftNoveltyRepository.findByWorkDateBetween(from, to);
        return new Context(from, to, codes, active, rows, novelties);
    }

    private static ShiftCode required(Map<String, ShiftCode> catalog, String code) {
        ShiftCode shiftCode = catalog.get(code);
        if (shiftCode == null) {
            throw new ShiftException(ShiftError.ROTATION_CODE_INACTIVE, code);
        }
        return shiftCode;
    }

    private record Range(LocalDate from, LocalDate to) {

        boolean isEmpty() {
            return from.isAfter(to);
        }
    }

    /** Data read for one plan. The titular role is the role of the DIA code, so no role name is written here. */
    private record Context(LocalDate from, LocalDate to, RotationPlanner.Codes codes,
                           Map<UUID, ScheduleWorkerInfo> active, List<ShiftAssignment> rows,
                           List<ShiftNovelty> novelties) {

        boolean isTitular(ScheduleWorkerInfo worker) {
            return codes.day().getRole().getId().equals(worker.roleId());
        }

        List<ScheduleWorkerInfo> titulares() {
            return active.values().stream()
                    .filter(this::isTitular)
                    .sorted(Comparator.comparing(ScheduleWorkerInfo::fullName))
                    .toList();
        }

        Plan plan(List<ScheduleWorkerInfo> toGenerate, Map<UUID, Integer> chosen, boolean replaceManual) {
            Map<UUID, Map<LocalDate, ShiftAssignment>> grid = new HashMap<>();
            rows.forEach(row -> grid.computeIfAbsent(row.getWorkerId(), id -> new HashMap<>()).put(row.getWorkDate(), row));
            Set<UUID> supportWorkers = active.values().stream()
                    .filter(worker -> !isTitular(worker))
                    .map(ScheduleWorkerInfo::id)
                    .collect(Collectors.toSet());
            Set<LocalDate> noveltyDates = novelties.stream()
                    .map(ShiftNovelty::getWorkDate)
                    .collect(Collectors.toCollection(TreeSet::new));

            List<RotationPreview.TitularPhase> phases = new ArrayList<>();
            Map<UUID, Integer> phaseByWorker = new LinkedHashMap<>();
            for (ScheduleWorkerInfo titular : toGenerate) {
                RotationPhaseDeducer.Deduction deduction =
                        RotationPhaseDeducer.deduce(from, grid.getOrDefault(titular.id(), Map.of()));
                Integer provided = chosen.get(titular.id());
                PhaseSource source = provided != null ? PhaseSource.PROVIDED
                        : deduction.deduced() ? PhaseSource.DEDUCED : PhaseSource.SUGGESTED;
                int phase = provided != null ? provided : deduction.phase();
                phases.add(new RotationPreview.TitularPhase(titular.id(), titular.fullName(), phase, source,
                        source == PhaseSource.SUGGESTED, deduction.errors()));
                phaseByWorker.put(titular.id(), phase);
            }
            List<RotationPlanner.Proposal> proposals = RotationPlanner.plan(from, to, phaseByWorker, grid,
                    supportWorkers, noveltyDates, replaceManual, codes);
            String version = ScheduleVersion.of(rows,
                    titulares().stream().map(ScheduleWorkerInfo::id).toList(), novelties);
            return new Plan(this, phases, proposals, grid, supportWorkers, noveltyDates, version);
        }
    }

    /** A calculated plan with its proposed grid, coverage, warnings and counts. */
    private static final class Plan {

        private final Context context;
        private final List<RotationPreview.TitularPhase> phases;
        private final List<RotationPlanner.Proposal> proposals;
        private final Set<UUID> supportWorkers;
        private final Set<LocalDate> noveltyDates;
        private final String version;
        private final List<RotationPreview.Day> days = new ArrayList<>();
        private final List<LocalDate> uncoveredDates = new ArrayList<>();

        Plan(Context context, List<RotationPreview.TitularPhase> phases, List<RotationPlanner.Proposal> proposals,
             Map<UUID, Map<LocalDate, ShiftAssignment>> grid, Set<UUID> supportWorkers, Set<LocalDate> noveltyDates,
             String version) {
            this.context = context;
            this.phases = phases;
            this.proposals = proposals;
            this.supportWorkers = supportWorkers;
            this.noveltyDates = noveltyDates;
            this.version = version;
            buildDays(grid);
        }

        List<RotationPlanner.Proposal> proposals() {
            return proposals;
        }

        String version() {
            return version;
        }

        List<LocalDate> uncoveredDates() {
            return List.copyOf(uncoveredDates);
        }

        RotationPreview.Counts counts() {
            int generated = (int) proposals.stream().filter(RotationPlanner.Proposal::writes).count();
            return new RotationPreview.Counts(generated, proposals.size() - generated, uncoveredDates.size());
        }

        /** A titular whose phase could not be deduced must get it from the administrator before saving. */
        void requirePhases() {
            phases.stream().filter(RotationPreview.TitularPhase::needsChoice).findFirst().ifPresent(phase -> {
                throw new ShiftException(ShiftError.PHASE_REQUIRED, phase.fullName(), phase.phase());
            });
        }

        /** Proposed grid of every day: the titulares of the plan plus the current rows of the other active workers. */
        private void buildDays(Map<UUID, Map<LocalDate, ShiftAssignment>> grid) {
            Map<LocalDate, Map<UUID, RotationPlanner.Proposal>> byDate = new HashMap<>();
            proposals.forEach(proposal ->
                    byDate.computeIfAbsent(proposal.date(), date -> new HashMap<>()).put(proposal.workerId(), proposal));
            List<ScheduleWorkerInfo> workers = context.active().values().stream()
                    .sorted(Comparator.comparing((ScheduleWorkerInfo worker) -> !context.isTitular(worker))
                            .thenComparing(ScheduleWorkerInfo::fullName))
                    .toList();
            for (LocalDate date = context.from(); !date.isAfter(context.to()); date = date.plusDays(1)) {
                Map<UUID, RotationPlanner.Proposal> planned = byDate.getOrDefault(date, Map.of());
                List<RotationPreview.Row> rows = new ArrayList<>();
                List<ShiftAssignment> finalGrid = new ArrayList<>();
                for (ScheduleWorkerInfo worker : workers) {
                    RotationPlanner.Proposal proposal = planned.get(worker.id());
                    ShiftAssignment current = grid.getOrDefault(worker.id(), Map.of()).get(date);
                    RowStatus status = proposal != null ? proposal.status() : RowStatus.UNCHANGED;
                    ShiftCode code = proposal != null && proposal.writes() ? proposal.code()
                            : current == null ? null : current.getShiftCode();
                    if (code == null) {
                        continue;
                    }
                    ShiftOrigin origin = proposal != null && proposal.writes() ? ShiftOrigin.GENERATED : current.getOrigin();
                    rows.add(new RotationPreview.Row(worker.id(), worker.fullName(), code.getCode(), origin, status));
                    finalGrid.add(new ShiftAssignment(worker.id(), date, code, null));
                }
                RotationPreview.Coverage coverage = coverage(date, finalGrid);
                if (!coverage.covered()) {
                    uncoveredDates.add(date);
                }
                days.add(new RotationPreview.Day(date, rows, coverage));
            }
        }

        private static RotationPreview.Coverage coverage(LocalDate date, List<ShiftAssignment> finalGrid) {
            DailyCoverage daily = ScheduleCoverageCalculator.coverage(date, date, finalGrid).get(0);
            List<String> uncovered = StaffingAdvisor.uncoveredRanges(
                            finalGrid.stream().map(ShiftAssignment::getShiftCode).toList()).stream()
                    .map(range -> "%02d:00 a %02d:00".formatted(range[0], (range[1] + 1) % 24))
                    .toList();
            return new RotationPreview.Coverage(daily.covered() && uncovered.isEmpty(), daily.dayCovered(),
                    daily.nightCovered(), daily.supportCovered(), uncovered);
        }

        RotationPreview toPreview(YearMonth month) {
            Map<UUID, String> names = context.active().values().stream()
                    .collect(Collectors.toMap(ScheduleWorkerInfo::id, ScheduleWorkerInfo::fullName));
            List<RotationPreview.KeptDay> kept = proposals.stream()
                    .filter(proposal -> proposal.status() == RowStatus.KEPT_MANUAL)
                    .map(proposal -> new RotationPreview.KeptDay(proposal.workerId(), names.get(proposal.workerId()),
                            proposal.date(), proposal.existing().getShiftCode().getCode(), proposal.code().getCode()))
                    .toList();
            return new RotationPreview(month, context.from(), context.to(), phases, days, kept,
                    List.copyOf(noveltyDates), warnings(), counts(), version);
        }

        private List<RotationPreview.Warning> warnings() {
            List<RotationPreview.Warning> warnings = new ArrayList<>();
            if (phases.size() != EXPECTED_TITULARES) {
                warnings.add(new RotationPreview.Warning("TITULAR_COUNT", "Hay " + phases.size()
                        + " titulares activos; la rotación necesita 3 para cubrir día, noche y descanso cada día"));
            }
            if (!phasesTwoApart()) {
                List<String> missing = days.stream()
                        .filter(day -> !day.coverage().dayCovered() || !day.coverage().nightCovered())
                        .map(day -> day.date().toString())
                        .toList();
                warnings.add(new RotationPreview.Warning("PHASES_NOT_SEPARATED",
                        "Las fases de los titulares no están separadas por 2 posiciones"
                                + (missing.isEmpty() ? "" : "; días sin titular de día o de noche: "
                                + String.join(", ", missing))));
            }
            for (RotationPreview.Day day : days) {
                if (!day.coverage().covered()) {
                    warnings.add(new RotationPreview.Warning("UNCOVERED_DAY", day.date() + " sin cobertura completa: "
                            + describe(day.coverage())));
                }
            }
            boolean supportScheduled = days.stream()
                    .flatMap(day -> day.rows().stream())
                    .anyMatch(row -> supportWorkers.contains(row.workerId()) && !REST.equals(row.shiftCode()));
            if (!supportScheduled) {
                warnings.add(new RotationPreview.Warning("SUPPORT_NOT_SCHEDULED",
                        "El apoyo no tiene turnos asignados en el rango; los titulares de día quedan en DIA"));
            }
            return warnings;
        }

        /** Every pair of phases differs by 2 or 4 positions, so no two titulares share a kind of day. */
        private boolean phasesTwoApart() {
            for (int i = 0; i < phases.size(); i++) {
                for (int j = i + 1; j < phases.size(); j++) {
                    int distance = Math.floorMod(phases.get(i).phase() - phases.get(j).phase(),
                            RotationPhaseDeducer.CYCLE);
                    if (distance != 2 && distance != 4) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static String describe(RotationPreview.Coverage coverage) {
            List<String> problems = new ArrayList<>();
            if (!coverage.dayCovered()) {
                problems.add("sin titular de día");
            }
            if (!coverage.nightCovered()) {
                problems.add("sin titular de noche");
            }
            if (!coverage.supportCovered()) {
                problems.add("sin apoyo para un turno que lo necesita");
            }
            coverage.uncoveredHours().forEach(range -> problems.add("sin personal de " + range));
            return String.join("; ", problems);
        }
    }
}
