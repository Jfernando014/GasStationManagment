package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import static com.edu.unicauca.gasstation.backend.shifts.ShiftTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.shifts.ShiftTestData.TITULAR_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.shifts.ShiftTestData.assignment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleChange;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ScheduleView;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftOrigin;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.WorkerShifts;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The service calculates "today" with {@code LocalDate.now(America/Bogota)} and no injectable clock, so dates here
 * are relative to today in the same zone. Avoid running these tests exactly at midnight in Bogotá.
 */
@ExtendWith(MockitoExtension.class)
class ShiftScheduleServiceTest {

    private static final ZoneId STATION_ZONE = ZoneId.of("America/Bogota");

    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BRUNO_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CARLOS_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID LUISA_ID = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    private static final ScheduleWorkerInfo ANA = ShiftTestData.worker(ANA_ID, "Ana Villamil", TITULAR_ROLE_ID, true);
    private static final ScheduleWorkerInfo BRUNO = ShiftTestData.worker(BRUNO_ID, "Bruno Diaz", TITULAR_ROLE_ID, true);
    private static final ScheduleWorkerInfo CARLOS = ShiftTestData.worker(CARLOS_ID, "Carlos Restrepo", SUPPORT_ROLE_ID, true);
    private static final ScheduleWorkerInfo LUISA = ShiftTestData.worker(LUISA_ID, "Luisa Paz", TITULAR_ROLE_ID, false);

    /** Fixed days for the schedule queries, which accept any date. */
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_2 = DAY_1.plusDays(1);
    private static final LocalDate DAY_3 = DAY_1.plusDays(2);

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftCodeRepository shiftCodeRepository;

    @Mock
    private ShiftNoveltyRepository shiftNoveltyRepository;

    @Mock
    private ScheduleWorkerDirectory scheduleWorkerDirectory;

    @InjectMocks
    private ShiftScheduleService shiftScheduleService;

    private ShiftRotationService shiftRotationService;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(STATION_ZONE);
        shiftRotationService = new ShiftRotationService(shiftAssignmentRepository, shiftCodeRepository,
                shiftNoveltyRepository, scheduleWorkerDirectory);
    }

    // ---- assign ----

    @Test
    void assignRejectsAPastDateBeforeLookingUpAnything() {
        LocalDate yesterday = today.minusDays(1);

        assertThatThrownBy(() -> shiftScheduleService.assign(ANA_ID, yesterday, "DIA", null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.PAST_DATE))
                .hasMessage("La fecha " + yesterday + " ya pasó; solo se pueden programar turnos desde hoy");
        verifyNoInteractions(scheduleWorkerDirectory, shiftCodeRepository, shiftAssignmentRepository);
    }

    @Test
    void assignThrowsWhenWorkerIsInactive() {
        workersInDirectory(LUISA);

        assertThatThrownBy(() -> shiftScheduleService.assign(LUISA_ID, today, "DIA", null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.WORKER_INACTIVE));
        verify(shiftAssignmentRepository, never()).save(any());
    }

    @Test
    void assignThrowsWhenShiftCodeIsInactive() {
        workersInDirectory(ANA);
        ShiftCode inactiveDia = ShiftCode.builder()
                .code("DIA").name("Día").role(ShiftTestData.titularRole()).period(ShiftPeriod.DAY)
                .startHour1((short) 6).endHour1((short) 18).active(false).build();
        when(shiftCodeRepository.findByCode("DIA")).thenReturn(Optional.of(inactiveDia));

        assertThatThrownBy(() -> shiftScheduleService.assign(ANA_ID, today, "DIA", null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.SHIFT_CODE_NOT_FOUND));
        verify(shiftAssignmentRepository, never()).save(any());
    }

    @Test
    void assignThrowsWhenTheCodeBelongsToAnotherRole() {
        workersInDirectory(CARLOS);
        catalogWith("DIA");

        assertThatThrownBy(() -> shiftScheduleService.assign(CARLOS_ID, today, "DIA", null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.ROLE_MISMATCH))
                .hasMessage("El turno DIA es para el rol TITULAR y no corresponde al rol del vendedor");
        verify(shiftAssignmentRepository, never()).save(any());
    }

    @Test
    void assignAcceptsDescansoForASupportWorkerAndCreatesTheAssignment() {
        LocalDate tomorrow = today.plusDays(1);
        workersInDirectory(CARLOS);
        catalogWith("DESCANSO");
        when(shiftAssignmentRepository.findByWorkerIdAndWorkDate(CARLOS_ID, tomorrow)).thenReturn(Optional.empty());
        when(shiftAssignmentRepository.findByWorkDateBetween(tomorrow, tomorrow)).thenReturn(List.of());

        ScheduleChange change = shiftScheduleService.assign(CARLOS_ID, tomorrow, "DESCANSO", "  Permiso  ");

        ArgumentCaptor<ShiftAssignment> captor = ArgumentCaptor.forClass(ShiftAssignment.class);
        verify(shiftAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getShiftCode().getCode()).isEqualTo("DESCANSO");
        assertThat(captor.getValue().getNote()).isEqualTo("Permiso");
        assertThat(change.saved()).isEqualTo(1);
    }

    @Test
    void assignReplacesCodeAndNoteOfAnExistingAssignment() {
        ShiftAssignment existing = ShiftAssignment.restore(UUID.randomUUID(), ANA_ID, today,
                ShiftTestData.shiftCode("NOCHE"), "Anterior", ShiftOrigin.MANUAL);
        workersInDirectory(ANA);
        catalogWith("DIA");
        when(shiftAssignmentRepository.findByWorkerIdAndWorkDate(ANA_ID, today)).thenReturn(Optional.of(existing));
        when(shiftAssignmentRepository.findByWorkDateBetween(today, today)).thenReturn(List.of());

        shiftScheduleService.assign(ANA_ID, today, "DIA", "Cambio");

        verify(shiftAssignmentRepository, times(1)).save(existing);
        assertThat(existing.getShiftCode().getCode()).isEqualTo("DIA");
        assertThat(existing.getNote()).isEqualTo("Cambio");
    }

    @Test
    void assignReportsTheDayUncoveredWhenACodeNeedsSupportAndThereIsNone() {
        ScheduleChange change = assignWithStateAfter(List.of(
                assignment(ANA_ID, today, "DIA6"), assignment(BRUNO_ID, today, "NOCHE")));

        assertThat(change.uncoveredDates()).containsExactly(today);
    }

    @Test
    void assignLeavesTheDayCoveredWithAnySupportShift() {
        ScheduleChange change = assignWithStateAfter(List.of(
                assignment(ANA_ID, today, "DIA6"),
                assignment(BRUNO_ID, today, "NOCHE"),
                assignment(CARLOS_ID, today, "6-9am")));

        assertThat(change).isEqualTo(new ScheduleChange(1, List.of()));
    }

    // ---- getSchedule ----

    @Test
    void getScheduleRejectsAnEndBeforeTheStart() {
        assertThatThrownBy(() -> shiftScheduleService.getSchedule(DAY_2, DAY_1, null, null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.INVALID_DATE_RANGE))
                .hasMessage("La fecha final (" + DAY_1 + ") no puede ser anterior a la fecha inicial (" + DAY_2 + ")");
    }

    @Test
    void getScheduleWithoutAssignmentsHasEveryDayUncovered() {
        scheduleWith(DAY_1, DAY_2, List.of());

        ScheduleView view = shiftScheduleService.getSchedule(DAY_1, DAY_2, null, null);

        assertThat(view.workers()).isEmpty();
        assertThat(view.coverage()).containsExactly(
                new DailyCoverage(DAY_1, false, false, true),
                new DailyCoverage(DAY_2, false, false, true));
    }

    @Test
    void getScheduleShowsActiveWorkersOrderedByNameWithDaysOrderedByDate() {
        scheduleWith(DAY_1, DAY_3, List.of(
                assignment(BRUNO_ID, DAY_2, "NOCHE"),
                assignment(ANA_ID, DAY_3, "DIA"),
                assignment(ANA_ID, DAY_1, "DIA"),
                assignment(LUISA_ID, DAY_1, "NOCHE")), ANA, BRUNO, LUISA);

        ScheduleView view = shiftScheduleService.getSchedule(DAY_1, DAY_3, null, null);

        assertThat(view.workers()).extracting(WorkerShifts::fullName).containsExactly("Ana Villamil", "Bruno Diaz");
        assertThat(view.workers().get(0).assignments()).extracting(ShiftAssignment::getWorkDate)
                .containsExactly(DAY_1, DAY_3);
        assertThat(view.coverage().get(0).nightCovered()).as("an inactive worker does not cover").isFalse();
    }

    @Test
    void getScheduleFiltersByPeriodAndHidesDescanso() {
        scheduleWith(DAY_1, DAY_2, List.of(
                assignment(ANA_ID, DAY_1, "DIA"),
                assignment(ANA_ID, DAY_2, "DESCANSO"),
                assignment(BRUNO_ID, DAY_1, "NOCHE")), ANA, BRUNO);

        ScheduleView view = shiftScheduleService.getSchedule(DAY_1, DAY_2, null, ShiftPeriod.DAY);

        assertThat(view.workers()).singleElement().satisfies(worker ->
                assertThat(worker.assignments()).extracting(a -> a.getShiftCode().getCode()).containsExactly("DIA"));
    }

    @Test
    void filtersDoNotChangeTheCoverage() {
        scheduleWith(DAY_1, DAY_1, List.of(
                assignment(ANA_ID, DAY_1, "DIA6"),
                assignment(BRUNO_ID, DAY_1, "NOCHE"),
                assignment(CARLOS_ID, DAY_1, "12-7")), ANA, BRUNO, CARLOS);

        List<DailyCoverage> unfiltered = shiftScheduleService.getSchedule(DAY_1, DAY_1, null, null).coverage();
        List<DailyCoverage> filtered = shiftScheduleService.getSchedule(DAY_1, DAY_1, CARLOS_ID, ShiftPeriod.NIGHT)
                .coverage();

        assertThat(filtered).isEqualTo(unfiltered);
        assertThat(unfiltered.get(0).covered()).isTrue();
    }

    @Test
    void supportWorkerOrTitularOnDescansoDoNotCoverTheDay() {
        scheduleWith(DAY_1, DAY_1, List.of(
                assignment(CARLOS_ID, DAY_1, "12-7"),
                assignment(ANA_ID, DAY_1, "DESCANSO"),
                assignment(BRUNO_ID, DAY_1, "NOCHE")), ANA, BRUNO, CARLOS);

        ScheduleView view = shiftScheduleService.getSchedule(DAY_1, DAY_1, null, null);

        assertThat(view.coverage()).containsExactly(new DailyCoverage(DAY_1, false, true, true));
        verify(scheduleWorkerDirectory, times(1)).getWorkersByIds(anyCollection());
    }

    // ---- generateRotation ----

    @Test
    void generateRotationRejectsLessThanOneCycleBeforeLookingUpTheWorker() {
        LocalDate start = today.plusDays(1);

        assertThatThrownBy(() -> shiftRotationService.generateForWorker(ANA_ID, start, start.plusDays(4), null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.INVALID_ROTATION_LENGTH))
                .hasMessage("La rotación debe cubrir al menos un ciclo completo y como máximo un trimestre: si inicia el "
                        + start + ", la fecha final debe estar entre " + start.plusDays(5) + " y "
                        + start.plusMonths(3).minusDays(1));
        verifyNoInteractions(scheduleWorkerDirectory, shiftAssignmentRepository);
    }

    @Test
    void longestRangeFromTheLastDayOfNovemberEndsTheDayBeforeTheEndOfFebruary() {
        LocalDate start = nextNovember30();
        LocalDate maxEnd = YearMonth.of(start.getYear() + 1, 2).atEndOfMonth().minusDays(1);
        stubRotation(start, maxEnd);

        assertThat(shiftRotationService.generateForWorker(ANA_ID, start, maxEnd, 0).saved()).isPositive();
        assertThatThrownBy(() -> shiftRotationService.generateForWorker(ANA_ID, start, maxEnd.plusDays(1), 0))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.INVALID_ROTATION_LENGTH));
    }

    @Test
    void generateRotationIsNotAllowedForSupportWorkers() {
        LocalDate start = today.plusDays(1);
        workersInDirectory(CARLOS);
        activeCatalog();

        assertThatThrownBy(() -> shiftRotationService.generateForWorker(CARLOS_ID, start, start.plusDays(5), null))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.ROTATION_NOT_ALLOWED));
        verify(shiftAssignmentRepository, never()).saveAll(any());
    }

    @Test
    void generateRotationSavesTheTwoByTwoPatternWithoutDeletingTheRange() {
        LocalDate start = today.plusDays(1);
        LocalDate end = start.plusDays(13);
        stubRotation(start, end);

        ScheduleChange change = shiftRotationService.generateForWorker(ANA_ID, start, end, 0);

        InOrder order = inOrder(shiftAssignmentRepository);
        verify(shiftAssignmentRepository, never()).deleteByWorkerInRange(any(), any(), any());
        List<ShiftAssignment> rows = savedRotation(order);
        assertThat(change.saved()).isEqualTo(14);
        assertThat(rows).extracting(row -> row.getShiftCode().getCode()).containsExactly(
                "DIA", "DIA", "NOCHE", "NOCHE", "DESCANSO", "DESCANSO",
                "DIA", "DIA", "NOCHE", "NOCHE", "DESCANSO", "DESCANSO", "DIA", "DIA");
        assertThat(rows).extracting(ShiftAssignment::getWorkDate)
                .containsExactlyElementsOf(start.datesUntil(end.plusDays(1)).toList());
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getWorkerId()).isEqualTo(ANA_ID);
            assertThat(row.getNote()).isNull();
        });
        assertThat(change.uncoveredDates()).hasSize(14);
    }

    // ---- helpers ----

    /** The directory answers with the given workers whose id was asked for. */
    private void workersInDirectory(ScheduleWorkerInfo... workers) {
        Map<UUID, ScheduleWorkerInfo> byId = Arrays.stream(workers)
                .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
        when(scheduleWorkerDirectory.getWorkersByIds(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return byId.values().stream().filter(worker -> ids.contains(worker.id()))
                    .collect(Collectors.toMap(ScheduleWorkerInfo::id, Function.identity()));
        });
    }

    /** The shift code repository finds only the given catalog codes. */
    private void catalogWith(String... codes) {
        Set<String> available = Set.of(codes);
        when(shiftCodeRepository.findByCode(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return available.contains(code) ? Optional.of(ShiftTestData.shiftCode(code)) : Optional.empty();
        });
    }

    /** Ana gets DIA today; the repository returns {@code stateAfter} when the coverage is calculated. */
    private ScheduleChange assignWithStateAfter(List<ShiftAssignment> stateAfter) {
        workersInDirectory(ANA, BRUNO, CARLOS);
        catalogWith("DIA");
        when(shiftAssignmentRepository.findByWorkerIdAndWorkDate(ANA_ID, today)).thenReturn(Optional.empty());
        when(shiftAssignmentRepository.findByWorkDateBetween(today, today)).thenReturn(stateAfter);
        return shiftScheduleService.assign(ANA_ID, today, "DIA", null);
    }

    private void scheduleWith(LocalDate from, LocalDate to, List<ShiftAssignment> rows, ScheduleWorkerInfo... workers) {
        workersInDirectory(workers);
        when(shiftAssignmentRepository.findByWorkDateBetween(from, to)).thenReturn(rows);
    }

    /** Ana is titular, the whole catalog is active and the grid has no rows (every day uncovered). */
    private void stubRotation(LocalDate start, LocalDate end) {
        workersInDirectory(ANA);
        activeCatalog();
        when(shiftAssignmentRepository.findByWorkDateBetween(start.minusDays(12), end)).thenReturn(List.of());
    }

    /** The eleven codes of the catalog are active. */
    private void activeCatalog() {
        when(shiftCodeRepository.findActive())
                .thenReturn(ShiftTestData.CATALOG_CODES.stream().map(ShiftTestData::shiftCode).toList());
    }

    @SuppressWarnings("unchecked")
    private List<ShiftAssignment> savedRotation(InOrder order) {
        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        order.verify(shiftAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    /** November 30 of this year, or of next year when it is already past. */
    private LocalDate nextNovember30() {
        LocalDate thisYear = MonthDay.of(11, 30).atYear(today.getYear());
        return thisYear.isBefore(today) ? thisYear.plusYears(1) : thisYear;
    }
}
